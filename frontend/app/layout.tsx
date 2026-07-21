import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Payment Operations Console",
  description: "Review synthetic payment state transitions with explicit controls and audit history."
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
