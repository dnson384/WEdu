export async function fetchClientUserProfile() {
  const res = await fetch('/api/auth/me');
  if (!res.ok) throw new Error('Không thể lấy thông tin user');
  return res.json();
}