import { icons } from "@/presentation/common/icons";

interface Data {
  message: string | null;
}

export default function Error({ message }: Data) {
  return (
    <>
      <section
        id="error-message"
        className="flex items-center gap-3 bg-red-100 rounded-lg px-4 py-2"
      >
        <div className="text-red-500">{icons.error}</div>
        <p className="text-red-500 text-sm">{message}</p>
      </section>
    </>
  );
}
