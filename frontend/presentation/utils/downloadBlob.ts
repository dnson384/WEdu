export function downloadBlob(blob: Blob, fileName: string): void {
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement("a");
  try {
    link.href = url;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
  } finally {
    link.remove();
    window.setTimeout(() => window.URL.revokeObjectURL(url), 1000);
  }
}
