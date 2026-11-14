/** UI discovery is not authorisation. Backend checks active school role and RLS. */
export const dashboardModules=[
 {href:"/dashboard/planning",title:"Lesson Planning",description:"Staff-only lesson preparation and teaching guides",roles:["admin","head","teacher"]},
 {href:"/dashboard/users",title:"Invite School Members",description:"Admin-only invitations for teachers, students and parents",roles:["admin"]},
 {href:"/dashboard/coursework",title:"Assignments",description:"Published coursework, submissions and feedback",roles:["admin","head","teacher","student","parent"]},
 {href:"/dashboard/progress",title:"Learning Progress",description:"Private learner and class performance",roles:["admin","head","teacher","student","parent"]},
 {href:"/dashboard/admin",title:"Administration",description:"School settings and security overview",roles:["admin","head"]},
 {href:"/dashboard/reports",title:"School Reports",description:"Whole-school reporting",roles:["admin","head"]},
 {href:"/dashboard/notices",title:"Messages & Notices",description:"Announcements for your role",roles:["admin","head","teacher","student","parent","bursar","clerk"]},
 {href:"/dashboard/fees",title:"Fee Invoices",description:"School-issued invoices",roles:["admin","head","bursar","student","parent"]},
 {href:"/dashboard/assessments",title:"Assessments & Results",description:"Record class marks or view authorised results",roles:["admin","head","teacher","student","parent"]},
 {href:"/dashboard/my-learning",title:"My Learning / My Children",description:"Personal or linked-family learning information",roles:["student","parent"]},
 {href:"/dashboard/calendar",title:"School Calendar",description:"Events and sports programme",roles:["admin","head","teacher","student","parent","bursar","clerk"]},
 {href:"/dashboard/learners",title:"Learner Register",description:"Authorised class learner register",roles:["admin","head","teacher"]},
 {href:"/dashboard/teachers",title:"Teaching Staff",description:"Restricted staff register",roles:["admin","head"]},
 {href:"/dashboard/parents",title:"Guardian Directory",description:"Restricted family directory",roles:["admin","head"]},
 {href:"/dashboard/attendance",title:"Class Attendance",description:"Staff-only register marking",roles:["admin","head","teacher"]},
 {href:"/dashboard/learning",title:"Learning Library",description:"Lessons and school resources",roles:["admin","head","teacher","student","parent"]},
 {href:"/dashboard/curriculum",title:"Curriculum Lessons & Practice",description:"63 syllabus-aligned units and self-checks",roles:["admin","head","teacher","student","parent"]}
] as const;
export function visibleModules(role:string){return dashboardModules.filter(m=>(m.roles as readonly string[]).includes(role));}
export function canOpenDashboardModule(role:string,href:string){return visibleModules(role).some(m=>m.href===href);}
