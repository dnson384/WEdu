package com.wedu.exam_creation.user.infrastructure.repository;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.mongodb.client.result.DeleteResult;
import com.wedu.exam_creation.common.dto.user.request.UserUpdateFields;
import com.wedu.exam_creation.common.exception.ConflictException;
import com.wedu.exam_creation.common.exception.InternalServerException;
import com.wedu.exam_creation.user.domain.entity.UserEntity;
import com.wedu.exam_creation.user.infrastructure.document.UserDocument;
import com.wedu.exam_creation.user.infrastructure.mapper.UserMapper;
import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

class UserRepositoryImplTest {
    private static final String ID = "507f1f77bcf86cd799439011";
    private static final String EMAIL = "teacher@example.com";
    private static final Instant EXPIRES_AT = Instant.parse("2026-02-01T00:00:00Z");

    private final MongoTemplate mongo = mock(MongoTemplate.class);
    private final UserMapper mapper = mock(UserMapper.class);
    private final UserRepositoryImpl users = new UserRepositoryImpl(mongo, mapper);

    @Test
    @DisplayName("trả về tài khoản có mật khẩu mới và giữ quyền PLUS khi cập nhật mật khẩu")
    void test_updatePassword_001() {
        UserEntity expected = stubUpdatedDocument();

        UserEntity result = users.updatePassword(ID, "new-hash");

        assertThat(result).isSameAs(expected);
        assertThat(captureAccountUpdate())
                .containsOnlyKeys("hashedPassword", "updatedAt")
                .containsEntry("hashedPassword", "new-hash");
    }

    @Test
    @DisplayName("trả về tài khoản có vai trò mới và giữ quyền PLUS khi cập nhật vai trò")
    void test_updateRole_002() {
        UserEntity expected = stubUpdatedDocument();

        UserEntity result = users.updateRole(ID, "ROLE_MODERATOR");

        assertThat(result).isSameAs(expected);
        assertThat(captureAccountUpdate())
                .containsOnlyKeys("role", "updatedAt")
                .containsEntry("role", "ROLE_MODERATOR");
    }

    @Test
    @DisplayName("trả về tài khoản tìm được khi chuẩn hóa email trước khi hỏi MongoDB")
    void test_findByEmail_003() {
        UserDocument document = paidDocument();
        UserEntity expected = stubMappedId(document);
        when(mongo.findOne(any(Query.class), eq(UserDocument.class))).thenReturn(document);

        assertThat(users.findByEmail(" Teacher@Example.COM ")).containsSame(expected);

        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(mongo).findOne(query.capture(), eq(UserDocument.class));
        assertThat(query.getValue().getQueryObject()).isEqualTo(new Document("email", EMAIL));
    }

    @Test
    @DisplayName("trả về Optional rỗng khi MongoDB không tìm thấy email")
    void test_findByEmail_004() {
        assertThat(users.findByEmail(EMAIL)).isEmpty();
    }

    @Test
    @DisplayName("trả về tài khoản đã lưu với email chuẩn hóa và ID được cấp khi tạo người dùng")
    void test_save_005() {
        UserEntity pending = new UserEntity();
        pending.setEmail(" Teacher@Example.COM ");
        UserDocument submitted = new UserDocument();
        UserDocument persisted = paidDocument();
        UserEntity expected = stubMappedId(persisted);
        when(mapper.toDocument(pending)).thenReturn(submitted);
        when(mongo.save(submitted)).thenReturn(persisted);

        UserEntity saved = users.save(pending);

        assertThat(saved).isSameAs(expected);
        ArgumentCaptor<UserDocument> document = ArgumentCaptor.forClass(UserDocument.class);
        verify(mongo).save(document.capture());
        assertThat(document.getValue().getEmail()).isEqualTo(EMAIL);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MIXED", "GOOGLE"})
    @DisplayName(
            "trả về tài khoản đã liên kết khi MongoDB cập nhật đúng subject và phương thức đăng nhập")
    void test_linkGoogle_006(String method) {
        UserEntity expected = stubUpdatedDocument();

        UserEntity result = users.linkGoogle(ID, "subject", method);

        assertThat(result).isSameAs(expected);
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
        verify(mongo)
                .findAndModify(
                        query.capture(),
                        update.capture(),
                        argThat(FindAndModifyOptions::isReturnNew),
                        eq(UserDocument.class));
        List<String> loginMethods =
                method.equals("MIXED") ? List.of("LOCAL", "MIXED") : List.of("GOOGLE");
        assertThat(query.getValue().getQueryObject())
                .containsEntry("_id", ID)
                .containsEntry("role", "ROLE_TEACHER")
                .containsEntry("isActive", true)
                .containsEntry("loginMethod", new Document("$in", loginMethods))
                .containsEntry(
                        "$or",
                        List.of(
                                new Document("googleSubject", null),
                                new Document("googleSubject", "subject")));
        assertThat(update.getValue().getUpdateObject().get("$set", Document.class))
                .containsOnlyKeys("googleSubject", "loginMethod", "updatedAt")
                .containsEntry("googleSubject", "subject")
                .containsEntry("loginMethod", method);
    }

    @Test
    @DisplayName(
            "trả về ConflictException khi không có tài khoản thỏa điều kiện ghi liên kết Google")
    void test_linkGoogle_007() {
        assertThatThrownBy(() -> users.linkGoogle(ID, "subject", "MIXED"))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Không thể liên kết tài khoản Google");
        verify(mongo, never()).save(any(UserDocument.class));
    }

    @Test
    @DisplayName("trả về tài khoản có loại PLUS và hạn mới khi lưu quyền đã mua")
    void test_grantPlus_008() {
        UserEntity expected = stubUpdatedDocument();

        UserEntity result = users.grantPlus(ID, EXPIRES_AT);

        assertThat(result).isSameAs(expected);
        assertThat(captureAccountUpdate())
                .containsOnlyKeys("accountType", "plusExpiresAt", "updatedAt")
                .containsEntry("accountType", "PLUS")
                .containsEntry("plusExpiresAt", EXPIRES_AT);
    }

    @Test
    @DisplayName("trả về null khi người dùng cần cấp PLUS không còn tồn tại")
    void test_grantPlus_009() {
        assertThat(users.grantPlus(ID, EXPIRES_AT)).isNull();
    }

    @Test
    @DisplayName("trả về tài khoản đã cập nhật khi lưu avatar, tên và trạng thái được cung cấp")
    void test_updateField_010() {
        UserEntity expected = stubUpdatedDocument();

        UserEntity result =
                users.updateField(ID, new UserUpdateFields("avatars/new.png", "Tên mới", false));

        assertThat(result).isSameAs(expected);
        assertThat(captureAccountUpdate())
                .containsOnlyKeys("avatarUrl", "username", "isActive", "updatedAt")
                .containsEntry("avatarUrl", "avatars/new.png")
                .containsEntry("username", "Tên mới")
                .containsEntry("isActive", false);
    }

    @Test
    @DisplayName("trả về tên mới và giữ các trường khác khi chỉ cập nhật username")
    void test_updateField_011() {
        UserEntity expected = stubUpdatedDocument();

        assertThat(users.updateField(ID, new UserUpdateFields(null, "Tên mới", null)))
                .isSameAs(expected);

        assertThat(captureAccountUpdate())
                .containsOnlyKeys("username", "updatedAt")
                .containsEntry("username", "Tên mới");
    }

    @Test
    @DisplayName("trả về null khi cập nhật trường cho ID không khớp người dùng nào")
    void test_updateField_012() {
        assertThat(users.updateField(ID, new UserUpdateFields(null, "Tên mới", null))).isNull();
    }

    @Test
    @DisplayName("trả về người dùng đã ánh xạ khi MongoDB tìm thấy tài khoản theo ID")
    void test_findById_013() {
        UserDocument document = paidDocument();
        UserEntity expected = stubMappedId(document);
        when(mongo.findById(ID, UserDocument.class)).thenReturn(document);

        assertThat(users.findById(ID)).isSameAs(expected);
    }

    @Test
    @DisplayName("trả về null khi MongoDB không tìm thấy tài khoản theo ID")
    void test_findById_014() {
        assertThat(users.findById(ID)).isNull();
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 1})
    @DisplayName(
            "trả về kết quả đã xóa hay chưa khi truy vấn xóa bằng ObjectId ảnh hưởng tối đa một tài khoản")
    void test_delete_015(long count) {
        when(mongo.remove(any(Query.class), eq(UserDocument.class)))
                .thenReturn(DeleteResult.acknowledged(count));

        assertThat(users.delete(ID)).isEqualTo(count == 1);

        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(mongo).remove(query.capture(), eq(UserDocument.class));
        assertThat(query.getValue().getQueryObject())
                .isEqualTo(new Document("_id", new ObjectId(ID)));
    }

    @Test
    @DisplayName("trả về InternalServerException khi MongoDB báo đã xóa nhiều hơn một tài khoản")
    void test_delete_016() {
        when(mongo.remove(any(Query.class), eq(UserDocument.class)))
                .thenReturn(DeleteResult.acknowledged(2));

        assertThatThrownBy(() -> users.delete(ID)).isInstanceOf(InternalServerException.class);
    }

    @Test
    @DisplayName("trả về danh sách tài khoản đã ánh xạ khi MongoDB có nhiều người dùng")
    void test_all_017() {
        UserDocument second = new UserDocument();
        second.setId("other-user");
        UserDocument first = paidDocument();
        when(mongo.findAll(UserDocument.class)).thenReturn(List.of(first, second));
        stubMappedId(first);
        stubMappedId(second);

        assertThat(users.all()).extracting(UserEntity::getId).containsExactly(ID, "other-user");
    }

    @Test
    @DisplayName("trả về danh sách rỗng khi MongoDB không có người dùng")
    void test_all_018() {
        when(mongo.findAll(UserDocument.class)).thenReturn(List.of());

        assertThat(users.all()).isEmpty();
    }

    @Test
    @DisplayName(
            "trả về người dùng khớp khi tìm nguyên văn theo email hoặc tên mà không phân biệt hoa thường")
    void test_findByKeyword_019() {
        UserDocument document = paidDocument();
        when(mongo.find(any(Query.class), eq(UserDocument.class))).thenReturn(List.of(document));
        stubMappedId(document);

        assertThat(users.findByKeyword("A.B")).extracting(UserEntity::getId).containsExactly(ID);

        List<Document> criteria = captureSearchCriteria();
        assertThat(criteria).hasSize(2);
        Pattern email = criteria.get(0).get("email", Pattern.class);
        Pattern name = criteria.get(1).get("username", Pattern.class);
        assertThat(email.matcher("a.b@example.com").find()).isTrue();
        assertThat(email.matcher("acb@example.com").find()).isFalse();
        assertThat(name.matcher("Giáo viên a.b").find()).isTrue();
        assertThat(name.matcher("Giáo viên acb").find()).isFalse();
    }

    @Test
    @DisplayName(
            "trả về kết quả rỗng khi không tìm thấy người dùng theo từ khóa gồm ObjectId hợp lệ")
    void test_findByKeyword_020() {
        when(mongo.find(any(Query.class), eq(UserDocument.class))).thenReturn(List.of());

        assertThat(users.findByKeyword(ID)).isEmpty();

        assertThat(captureSearchCriteria())
                .hasSize(3)
                .contains(new Document("_id", new ObjectId(ID)));
    }

    private List<Document> captureSearchCriteria() {
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(mongo).find(query.capture(), eq(UserDocument.class));
        return query.getValue().getQueryObject().getList("$or", Document.class);
    }

    private Document captureAccountUpdate() {
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
        verify(mongo)
                .findAndModify(
                        query.capture(),
                        update.capture(),
                        argThat(FindAndModifyOptions::isReturnNew),
                        eq(UserDocument.class));
        assertThat(query.getValue().getQueryObject()).isEqualTo(new Document("_id", ID));
        verify(mongo, never()).save(any(UserDocument.class));
        return update.getValue().getUpdateObject().get("$set", Document.class);
    }

    private UserEntity stubMappedId(UserDocument document) {
        UserEntity user = new UserEntity();
        user.setId(document.getId());
        when(mapper.toEntity(document)).thenReturn(user);
        return user;
    }

    private UserEntity stubUpdatedDocument() {
        UserDocument updated = paidDocument();
        when(mongo.findAndModify(
                        any(Query.class),
                        any(Update.class),
                        any(FindAndModifyOptions.class),
                        eq(UserDocument.class)))
                .thenReturn(updated);
        return stubMappedId(updated);
    }

    private UserDocument paidDocument() {
        UserDocument document = new UserDocument();
        document.setId(ID);
        document.setEmail(EMAIL);
        document.setAccountType("PLUS");
        document.setPlusExpiresAt(EXPIRES_AT);
        return document;
    }
}
