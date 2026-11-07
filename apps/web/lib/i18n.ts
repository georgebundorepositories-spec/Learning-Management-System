export const languages = ["en", "sn", "nd"] as const;
export type Language = (typeof languages)[number];
export const messages = {
  en: {
    welcome:"Welcome to ZimSchool", subtitle:"School information, wherever you are.",
    identifier:"Mobile number or email", password:"Password", signIn:"Sign in",
    signingIn:"Signing in…", register:"Registration is by invitation only.",
    forgot:"Ask your school administrator for account access.",
    language:"Language", invalid:"Sign-in unsuccessful. Check your details and try again.",
    dashboard:"School dashboard", pending:"Account setup is not complete. Contact your school administrator.",
    signOut:"Sign out", school:"School", role:"Role", calendar:"Calendar",
    privacy:"Your school records are private and only available to authorised accounts."
  },
  sn: {
    welcome:"Titambire paZimSchool", subtitle:"Mashoko echikoro, chero kwaunenge uri.",
    identifier:"Nhamba yefoni kana email", password:"Pasiwedhi", signIn:"Pinda",
    signingIn:"Tiri kukupinzai…", register:"Kunyoresa kunoitwa nekukokwa chete.",
    forgot:"Bvunza mukuru wechikoro kuti akubatsire kupinda.",
    language:"Mutauro", invalid:"Kupinda kwaramba. Ongorora mashoko ako uedze zvakare.",
    dashboard:"Peji rechikoro", pending:"Akaunti yako haisati yapera kugadzirwa. Bata mukuru wechikoro.",
    signOut:"Buda", school:"Chikoro", role:"Basa", calendar:"Karenda",
    privacy:"Mashoko echikoro anochengetedzwa uye anoonekwa nevane mvumo chete."
  },
  nd: {
    welcome:"Siyakwamukela kuZimSchool", subtitle:"Ulwazi lwesikolo, loba ungaphi.",
    identifier:"Inombolo yocingo kumbe i-imeyili", password:"Iphasiwedi", signIn:"Ngena",
    signingIn:"Siyakungenisa…", register:"Ukubhalisa kwenziwa ngesimemo kuphela.",
    forgot:"Buza umphathi wesikolo ukuze uthole usizo lokungena.",
    language:"Ulimi", invalid:"Ukungena akuphumelelanga. Hlola imininingwane yakho uzame futhi.",
    dashboard:"Ikhasi lesikolo", pending:"I-akhawunti yakho ayikaqedwa. Thinta umphathi wesikolo.",
    signOut:"Phuma", school:"Isikolo", role:"Umsebenzi", calendar:"Ikhalenda",
    privacy:"Ulwazi lwesikolo luvikelwe njalo lubonwa ngabalemvumo kuphela."
  }
};
export function t(lang: Language) { return messages[lang]; }
