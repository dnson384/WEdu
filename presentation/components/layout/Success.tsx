import { icons } from "@/presentation/common/icons";

interface Data {
  message: string | null;
}

export default function Success({ message }: Data) {
  return (
    <>
      <section
        id="success-message"
        className="flex items-center gap-3 bg-green-100 rounded-lg px-4 py-2"
      >
        <div className="text-green-600">{icons.success}</div>
        <p className="text-green-600 text-sm">{message}</p>
      </section>
    </>
  );
}
