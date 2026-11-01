export function getRoleHome(role: string): string {
  if (role === "ROLE_ADMIN") return "/admin/users";
  if (role === "ROLE_MODERATOR") return "/moderator/question-files";
  return "/";
}
