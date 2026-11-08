import "./globals.css";
import type { Metadata } from "next";
import PwaRegistrar from "@/components/PwaRegistrar";

export const metadata: Metadata = {
  title: "ZimSchool LMS",
  description: "Offline-first learning management for Zimbabwean schools",
  manifest: "/manifest.webmanifest",
  icons: [{ rel: "icon", url: "/icon.svg", type: "image/svg+xml" }],
  robots: { index: false, follow: false }
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="en"><body>{children}<PwaRegistrar /></body></html>;
}
