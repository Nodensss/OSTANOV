import type { Metadata } from "next";
import { IBM_Plex_Mono, IBM_Plex_Sans, Rubik_Mono_One } from "next/font/google";
import { AppFrame } from "@/components/layout/AppFrame";
import "./globals.css";

const plexSans = IBM_Plex_Sans({
  subsets: ["cyrillic", "latin"],
  variable: "--font-plex-sans",
  weight: ["300", "400", "500", "600", "700"],
});

const plexMono = IBM_Plex_Mono({
  subsets: ["cyrillic", "latin"],
  variable: "--font-plex-mono",
  weight: ["400", "500", "600"],
});

const rubikMono = Rubik_Mono_One({
  subsets: ["cyrillic", "latin"],
  variable: "--font-rubik-mono",
  weight: "400",
});

export const metadata: Metadata = {
  title: "УПЭ Стоп",
  description: "Интерактивный гид по алгоритму остановки технологической линии УПЭ",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ru">
      <body
        className={`${plexSans.variable} ${plexMono.variable} ${rubikMono.variable} bg-bg text-text antialiased`}
      >
        <AppFrame>{children}</AppFrame>
      </body>
    </html>
  );
}
