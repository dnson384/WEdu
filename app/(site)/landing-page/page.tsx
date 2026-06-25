"use client";
import NavBarLanding from "@/presentation/components/layout/NavBarLanding";
import { icons } from "@/presentation/common/icons";

export default function Landing() {
  return (
    <>
      <NavBarLanding />
      <main className="mt-32">
        {/* Over view */}
        <section className="flex flex-col items-center justify-center ">
          <div className="flex flex-col items-center gap-6">
            <h4 className="w-fit bg-blue-50 py-2 px-4 rounded-full text-lg">
              Tuân thủ công văn 7991 của Bộ giáo dục
            </h4>
            <h2 className="text-3xl">
              Tạo đề kiểm tra tự dộng, Nhanh chóng và Chuyên nghiệp
            </h2>
            <p className="text-gray-500 text-lg text-center mb-8">
              Giải pháp toàn diện giúp giáo viên tạo đề kiểm tra theo ma trận
              chuẩn.
              <br />
              Tiết kiệm thời gian và đảm bảo chất lượng đánh giá học sinh.
            </p>
          </div>

          <div className="flex">
            <button className="text-lg min-w-40 py-3 bg-blue-500 rounded-xl text-white hover:bg-blue-600">
              Bắt đầu ngay
            </button>
          </div>
        </section>

        {/* Features */}
        <section id="features" className="mt-10 bg-blue-50/50 p-10 flex flex-col gap-10 items-center">
          <div>
            <h2 className="text-2xl text-center mb-5">Tính Năng Nổi Bật</h2>
            <p className="text-gray-500 text-lg">
              Tất cả các công cụ cần thiết để tạo và quản lý đề kiểm tra chuyên
              nghiệp
            </p>
          </div>

          <div className="grid grid-cols-3 gap-5 max-w-7xl">
            <div className="flex flex-col gap-3 bg-white border border-gray-200 rounded-xl p-6 hover:shadow-md transition duration-300 ease-in-out">
              <div className="w-12 h-12 bg-blue-100 rounded-md flex items-center justify-center">
                {icons.generate}
              </div>
              <h2 className="text-xl">Tự Động Tạo Đề</h2>
              <p className="text-gray-500">
                Tạo đề kiểm tra tự động dựa trên ngân hàng câu hỏi và ma trận đề
                đã thiết lập
              </p>
            </div>

            <div className="flex flex-col gap-3 bg-white border border-gray-200 rounded-xl p-6 hover:shadow-md transition duration-300 ease-in-out">
              <div className="w-12 h-12 bg-blue-100 rounded-md flex items-center justify-center">
                {icons.grid}
              </div>
              <h2 className="text-xl">Ma Trận Đề</h2>
              <p className="text-gray-500">
                Thiết kế ma trận đề theo chuẩn 7991, phân bổ câu hỏi theo mức độ
                nhận thức
              </p>
            </div>

            <div className="flex flex-col gap-3 bg-white border border-gray-200 rounded-xl p-6 hover:shadow-md transition duration-300 ease-in-out">
              <div className="w-12 h-12 bg-blue-100 rounded-md flex items-center justify-center">
                {icons.doc}
              </div>
              <h2 className="text-xl">Đặc Tả Ma Trận Đề</h2>
              <p className="text-gray-500">
                Xây dựng đặc tả chi tiết cho từng câu hỏi, đảm bảo đúng chuẩn
                kiến thức kỹ năng
              </p>
            </div>

            <div className="flex flex-col gap-3 bg-white border border-gray-200 rounded-xl p-6 hover:shadow-md transition duration-300 ease-in-out">
              <div className="w-12 h-12 bg-blue-100 rounded-md flex items-center justify-center">
                {icons.download}
              </div>
              <h2 className="text-xl">Xuất File Docx</h2>
              <p className="text-gray-500">
                Xuất đề thi dưới dạng file Word với định dạng chuẩn, sẵn sàng in
                ấn
              </p>
            </div>

            <div className="flex flex-col gap-3 bg-white border border-gray-200 rounded-xl p-6 hover:shadow-md transition duration-300 ease-in-out">
              <div className="w-12 h-12 bg-blue-100 rounded-md flex items-center justify-center">
                {icons.list}
              </div>
              <h2 className="text-xl">Quản Lý Danh Sách Đề</h2>
              <p className="text-gray-500">
                Lưu trữ và quản lý tất cả đề thi đã tạo, dễ dàng tìm kiếm và tái
                sử dụng
              </p>
            </div>

            <div className="flex flex-col gap-3 bg-white border border-gray-200 rounded-xl p-6 hover:shadow-md transition duration-300 ease-in-out">
              <div className="w-12 h-12 bg-blue-100 rounded-md flex items-center justify-center">
                {icons.upload}
              </div>
              <h2 className="text-xl">Ngân Hàng Câu Hỏi</h2>
              <p className="text-gray-500">
                Tải lên và quản lý ngân hàng câu hỏi theo chương, bài và mức độ
              </p>
            </div>
          </div>
        </section>

        {/* Prices */}
        <section className="mt-10 bg-white">
          <div className="mx-auto bg-white w-fit p-10 border border-1.5 ring-1 ring-blue-200 border-blue-100 rounded-2xl">
            <div className="mb-5">
              <h2 className="text-2xl text-center mb-3">
                Mô Hình Giá Minh Bạch
              </h2>
              <p className="text-gray-500 text-center">
                Chỉ trả phí cho những gì bạn thực sự cần
              </p>
            </div>

            <div className="grid grid-cols-2 gap-8">
              {/* Free */}
              <div className="bg-blue-50 px-6 py-8 rounded-xl w-lg">
                <div className="flex flex-col gap-2">
                  <div className="flex items-center gap-2 mb-2">
                    <div>{icons.tick32}</div>
                    <h3 className="text-xl">Miễn Phí Hoàn Toàn</h3>
                  </div>
                  <div className="flex items-center gap-2">
                    <div>{icons.tick24}</div>
                    <p className="text-gray-500">
                      Tạo đề tự động không giới hạn
                    </p>
                  </div>
                  <div className="flex items-center gap-2">
                    <div>{icons.tick24}</div>
                    <p className="text-gray-500">
                      Thiết kế ma trận đề và đặc tả
                    </p>
                  </div>
                  <div className="flex items-center gap-2">
                    <div>{icons.tick24}</div>
                    <p className="text-gray-500">Xuất file Docx</p>
                  </div>
                  <div className="flex items-center gap-2">
                    <div>{icons.tick24}</div>
                    <p className="text-gray-500">Quản lý danh sách đề</p>
                  </div>
                </div>
                <div></div>
              </div>

              {/* Cost */}
              <div className="border border-blue-100 px-6 py-8 rounded-xl w-lg flex flex-col gap-5">
                <div className="flex items-center">
                  <div>{icons.upload}</div>
                  <h3 className="text-xl">Tính Phí Hợp Lý</h3>
                </div>
                <p className="text-gray-500">
                  Chỉ tính phí khi bạn tải lên tài liệu ngân hàng câu hỏi mới
                </p>

                <div className="p-6 border border-gray-200 rounded-xl">
                  <h4 className="text-gray-500 text-lg mb-1">
                    Mỗi tài liệu tải lên:
                  </h4>
                  <p>Giá linh hoạt theo kích thước file</p>
                  <p>Không phí ẩn, không ràng buộc</p>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* Why to choose */}
        <section className="mt-10 bg-white">
          <div className="w-6xl mx-auto">
            <h2 className="text-2xl text-center mb-10">
              Tại sao lại chọn chúng tôi?
            </h2>
            <div className="grid grid-cols-3 gap-5">
              <div className="flex flex-col items-center gap-2">
                <div className="bg-blue-100 w-fit p-3 rounded-full mb-1">
                  {icons.shield}
                </div>
                <h3 className="text-xl">Tuân Thủ Công Văn 7991</h3>
                <p className="text-gray-500 text-center">
                  100% tuân thủ quy định của Bộ Giáo dục về đánh giá học sinh
                </p>
              </div>
              <div className="flex flex-col items-center gap-2">
                <div className="bg-blue-100 w-fit p-3 rounded-full mb-1">
                  {icons.clock}
                </div>
                <h3 className="text-xl">Tiết Kiệm Thời Gian</h3>
                <p className="text-gray-500 text-center">
                  Giảm 80% thời gian ra đề so với phương pháp truyền thống
                </p>
              </div>
              <div className="flex flex-col items-center gap-2">
                <div className="bg-blue-100 w-fit p-3 rounded-full mb-1">
                  {icons.tick32}
                </div>
                <h3 className="text-xl">Chất Lượng Đảm Bảo</h3>
                <p className="text-gray-500 text-center">
                  Đề thi cân bằng, đa dạng và phù hợp với chuẩn đầu ra
                </p>
              </div>
            </div>
          </div>
        </section>

        {/* Ready */}
        <section className="mt-10 py-20 bg-blue-50/50">
          <div className="flex flex-col items-center gap-5">
            <h2 className="text-2xl">Sẵn Sàng Bắt Đầu</h2>
            <p className="text-gray-500">
              Tham gia sử dụng hệ thống tạo đề tự động. Tạo đề miễn phí ngay hôm
              nay!
            </p>
            <button className="bg-blue-500 text-white px-8 py-3 rounded-xl hover:bg-blue-600">Bắt đầu miễn phí</button>
            <p className="text-gray-600">
              Không cần thẻ tín dụng • Chỉ trả phí khi tải tài liệu
            </p>
          </div>
        </section>

        {/* Footer */}
        <section className="py-8 border border-gray-300">
          <p className="text-center text-gray-500">
            © 2026 Hệ Thống Tạo Đề Tự Động - FckEdu. Tuân thủ chuẩn 7991 của Bộ
            Giáo dục.
          </p>
        </section>
      </main>
    </>
  );
}
