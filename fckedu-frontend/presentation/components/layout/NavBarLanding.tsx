import Logo from "../common/Logo";
import Link from "next/link";

export default function NavBarLanding() {
  return (
    <>
      <nav className="bg-white top-0 w-full shadow-md">
        <div className="max-w-6xl mx-auto py-6">
          <div className="flex items-center justify-between">
            <Logo />

            {/* Đăng ký & đăng nhập */}
            <div className="flex items-center gap-4">
              <Link
                href={"/auth/login"}
                className="flex items-center justify-center ring ring-blue-500 text-blue-500 rounded-lg w-30 py-2 hover:bg-blue-100"
                onClick={() => console.log("click")}
              >
                Đăng nhập
              </Link>
              <Link
                href={"/auth/register"}
                className="flex items-center justify-center bg-blue-500 text-white w-30 py-2 rounded-lg hover:bg-blue-600"
              >
                Đăng ký
              </Link>
            </div>
          </div>
        </div>
      </nav>
    </>
  );
}
