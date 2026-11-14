import { icons } from "@/presentation/common/icons";

export default function GoogleAuthButton({
  disabled = false,
}: {
  disabled?: boolean;
}) {
  return (
    <a
      href={disabled ? undefined : "/api/auth/google/start"}
      aria-disabled={disabled}
      className={`w-full py-2.5 border border-gray-300 rounded-lg flex items-center justify-center gap-2 font-medium text-gray-700 hover:bg-gray-50 transition-colors text-sm ${disabled ? "opacity-50 pointer-events-none" : "cursor-pointer"}`}
    >
      {icons.google}
      <span>Tiếp tục với Google</span>
    </a>
  );
}
