import "./globals.css";
import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "ZimSchool LMS",
  description: "Offline-first learning management for Zimbabwean schools",
  robots: { index: false, follow: false }
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="en"><body>{children}</body></html>;
}
