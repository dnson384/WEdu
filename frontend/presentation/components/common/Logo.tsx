import Link from "next/link";

export default function Logo() {
  return <Link href={"/"} className="flex items-center gap-1 select-none">
    <p className="text-blue-500 font-bold text-4xl">W</p>
    <p className="bg-blue-500 text-white px-3 py-1 rounded-md font-bold text-lg">Edu</p>
  </Link>;
}
