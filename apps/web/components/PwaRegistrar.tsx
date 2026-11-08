"use client";

import { useEffect } from "react";

/** Cache only public UI shells; never cache private dashboard or Auth API responses. */
export default function PwaRegistrar() {
  useEffect(() => {
    if (process.env.NODE_ENV !== "production" || !("serviceWorker" in navigator)) return;
    navigator.serviceWorker.register("/sw.js", { scope: "/" }).catch(() => {
      // Browsers without Service Worker must still offer online access.
    });
  }, []);
  return null;
}
