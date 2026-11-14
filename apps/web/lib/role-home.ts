import {canOpenDashboardModule} from "./portal-access";

export type SimpleLink={title:string;detail:string;href:string};
export type RoleHub={slug:string;title:string;description:string;links:readonly SimpleLink[]};
const link=(title:string,detail:string,href:string):SimpleLink=>({title,detail,href});
const hub=(slug:string,title:string,description:string,links:SimpleLink[]):RoleHub=>
 ({slug,title,description,links});

/** Five or fewer consistent, task-based destinations. No tables or fake buttons.
 * Every linked application module still requires its own server role guard + RLS.
 */
const roleHubs:Record<string,readonly RoleHub[]>={
 student:[
  hub("learning","My Learning","Syllabus lessons and class assignments",[
   link("My syllabus","Read lessons and practise questions","/dashboard/curriculum"),
   link("Assignments","Read and submit class work","/dashboard/coursework"),
   link("Learning materials","Read teacher-approved learning links","/dashboard/learning")]),
  hub("results","My Results","See marks and how learning is going",[
   link("My results","See private test and assessment marks","/dashboard/assessments"),
   link("My progress","See completed work and attendance","/dashboard/progress")]),
  hub("updates","School News & Events","Read school notices and upcoming events",[
   link("School news","Important messages from school","/dashboard/notices"),
   link("School calendar","Upcoming events and sports","/dashboard/calendar")]),
  hub("school-life","My School Life","Clubs, activities and safe student conversations",[
   link("Clubs and activities","View published activities and safeguarding status","/dashboard/school-life")]),
  hub("careers","My Future","Explore interests, subjects and possible careers",[
   link("Career guidance","Simple questions and career pathways","/dashboard/careers")])
 ],
 teacher:[
  hub("classroom","My Classroom","Attendance, learners and classes",[
   link("Take attendance","Record and review attendance","/dashboard/attendance"),
   link("My learners","See authorised learners in your classes","/dashboard/learners")]),
  hub("teaching","Plan & Teach","Lesson preparation and learning materials",[
   link("Lesson plans","Prepare and save lesson plans","/dashboard/planning"),
   link("Syllabus","Find curriculum lessons and activities","/dashboard/curriculum"),
   link("Share learning material","Add approved learning links","/dashboard/learning")]),
  hub("marking","Mark Work","Assignments, tests and results",[
   link("Assignments","Publish and review learners' answers","/dashboard/coursework"),
   link("Assessments","Record marks and feedback","/dashboard/assessments"),
   link("Class progress","Review permitted learning progress","/dashboard/progress")]),
  hub("updates","School News & Events","Notices and school calendar",[
   link("School notices","Read school news","/dashboard/notices"),
   link("School calendar","View school events","/dashboard/calendar")])
 ],
 parent:[
  hub("my-child","My Child","Private records for your linked learners",[
   link("My children","See linked children and attendance","/dashboard/my-learning"),
   link("Results and progress","See authorised marks and learning progress","/dashboard/progress"),
   link("Assessments","Review your children's formal results","/dashboard/assessments")]),
  hub("learning","Support Learning","Help your child study at home",[
   link("Syllabus","Read class lessons and practice","/dashboard/curriculum"),
   link("Class assignments","See work set by teachers","/dashboard/coursework")]),
  hub("updates","School News & Events","Find notices and event dates",[
   link("School news","See notices for your role","/dashboard/notices"),
   link("School calendar","See upcoming activities","/dashboard/calendar")]),
  hub("fees","School Fees","View invoices, not payment receipts",[
   link("Fee invoices","See school-issued invoices","/dashboard/fees")])
 ],
 admin:[
  hub("people","People & Classes","School members and class records",[
   link("Manage school members","Invite and manage authorised members","/dashboard/users"),
   link("Learners","See registered learners","/dashboard/learners"),
   link("Teachers","See teaching staff","/dashboard/teachers"),
   link("Families","See linked guardians","/dashboard/parents")]),
  hub("teaching","Teaching & Learning","Oversee classes, lessons and marking",[
   link("Syllabus & lessons","Access the school curriculum","/dashboard/curriculum"),
   link("Attendance","Review and mark class registers","/dashboard/attendance"),
   link("Assessments","Oversee tests and results","/dashboard/assessments"),
   link("Lesson planning","Prepare lesson plans","/dashboard/planning"),
   link("Assignments","Oversee class assignments","/dashboard/coursework")]),
  hub("updates","School News & Events","Notices and event information",[
   link("Publish notices","Publish school announcements","/dashboard/notices"),
   link("School calendar","View school events","/dashboard/calendar")]),
  hub("reports","School Overview","Progress, reports and school settings",[
   link("School reports","View available school summaries","/dashboard/reports"),
   link("Learner progress","Review authorised learner performance","/dashboard/progress"),
   link("School settings","Review school status and access","/dashboard/admin")]),
  hub("fees","School Fees","Issue and review fee invoices",[
   link("Fee invoices","Manage issued charges","/dashboard/fees")])
 ],
 head:[
  hub("people","People & Classes","Authorised learner and staff registers",[
   link("Learners","See registered learners","/dashboard/learners"),
   link("Teachers","See teaching staff","/dashboard/teachers"),
   link("Families","See linked guardians","/dashboard/parents")]),
  hub("teaching","Teaching & Learning","Lessons, assessments and attendance",[
   link("Syllabus & lessons","Access the school curriculum","/dashboard/curriculum"),
   link("Attendance","Review class registers","/dashboard/attendance"),
   link("Assessments","Oversee results","/dashboard/assessments"),
   link("Lesson planning","Review and create plans","/dashboard/planning")]),
  hub("updates","School News & Events","News and school programme",[
   link("School notices","Publish and read messages","/dashboard/notices"),
   link("Calendar","See school events","/dashboard/calendar")]),
  hub("reports","School Overview","School activity and progress",[
   link("School reports","See permitted statistics","/dashboard/reports"),
   link("Learning progress","See permitted learner summaries","/dashboard/progress"),
   link("School settings","See school overview","/dashboard/admin")]),
  hub("fees","School Fees","Review and issue invoices",[
   link("Fee invoices","View charges","/dashboard/fees")])
 ],
 bursar:[
  hub("fees","Fee Invoices","Review school-issued charges",[
   link("Fee invoices","See authorised invoices","/dashboard/fees")]),
  hub("updates","School News","Read school communications",[
   link("Notices","See school announcements","/dashboard/notices"),
   link("Calendar","See approved school events","/dashboard/calendar")])
 ],
 clerk:[
  hub("updates","School News & Events","Read authorised announcements and events",[
   link("Notices","Read school news","/dashboard/notices"),
   link("Calendar","See approved events","/dashboard/calendar")])
 ]
};
const safeNewPages=new Set(["/dashboard/school-life","/dashboard/careers"]);
export function homeHubs(role:string):readonly RoleHub[]{
 return (roleHubs[role]??[]).map(group=>({
  ...group,links:group.links.filter(item=>safeNewPages.has(item.href)
   ? role==="student"
   : canOpenDashboardModule(role,item.href))
 })).filter(group=>group.links.length>0);
}
export function homeHub(role:string,slug:string):RoleHub|undefined{
 return homeHubs(role).find(item=>item.slug===slug);
}
