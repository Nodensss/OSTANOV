import type { MetadataRoute } from "next";

export default function manifest(): MetadataRoute.Manifest {
  return {
    name: "УПЭ Стоп",
    short_name: "УПЭ Стоп",
    description: "Интерактивный алгоритм остановки технологической линии УПЭ",
    start_url: "/",
    display: "standalone",
    background_color: "#0a0d0f",
    theme_color: "#60a5fa",
  };
}
