import { icons } from "@/presentation/common/icons";

interface Data {
  message: string | null;
}

export default function Noti({ message }: Data) {
  return (
    <>
      <section
        id="noti-message"
        className="flex items-center gap-3 bg-amber-50 rounded-lg px-4 py-2"
      >
        <div className="text-amber-600">{icons.warning}</div>
        <p className="text-amber-600 text-sm">{message}</p>
      </section>
    </>
  );
}
