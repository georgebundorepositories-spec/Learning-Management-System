import type {CurriculumDeepDive} from "@/lib/curriculum/deep-dive";

/**
 * Learner-facing original enrichment. Only hints are revealed here.
 * Expected answers stay on the server and are shown below in the staff-only
 * marking guide; formative exercises are not recorded as formal grades.
 */
export default function DeepLesson({
  content,isStaff
}:{
  content:CurriculumDeepDive;
  isStaff:boolean;
}){
  return <div className="deep-lesson" aria-label="Deeper learning activities">
    <div className="summary-card deep-lesson-intro">
      <p className="eyebrow">DEEPER LESSON • THINK, TRY, EXPLAIN</p>
      <h2>Before we begin</h2>
      <p>{content.starter}</p>
    </div>
    <section className="summary-card">
      <h2>Words you will use</h2>
      <dl className="deep-vocabulary">{content.vocabulary.map(([term,definition])=>
        <div key={term}><dt>{term}</dt><dd>{definition}</dd></div>)}</dl>
    </section>
    <section className="summary-card">
      <h2>Learn it in three stages</h2>
      <ol className="deep-steps">{content.steps.map(([title,explanation],i)=>
        <li key={title}><h3>Stage {i+1}: {title}</h3><p>{explanation}</p></li>)}</ol>
    </section>
    <section className="summary-card">
      <h2>Follow a worked solution</h2>
      <ol className="deep-steps">{content.worked.map((stage,index)=>
        <li key={index}><p>{stage}</p></li>)}</ol>
    </section>
    <section className="summary-card">
      <h2>Practise with a helpful clue</h2>
      <p className="privacy-note">Think or write your response before opening a clue. These are practice activities, not graded tests.</p>
      <ol className="deep-practice">{content.guided.map(([prompt,hint],i)=>
        <li key={i}><p>{prompt}</p>
          <details><summary>Need a clue?</summary><p>{hint}</p></details>
        </li>)}</ol>
    </section>
    <section className="summary-card">
      <h2>Your turn: independent practice</h2>
      <ol className="deep-practice">{content.practice.map(([prompt],i)=>
        <li key={i}>{prompt}</li>)}</ol>
      <p className="privacy-note">Show your reasoning, diagrams or working. Ask your teacher to discuss your answers.</p>
    </section>
    <section className="summary-card">
      <h2>Watch out for this mistake</h2><p>{content.misconception}</p>
      <h3>Apply your learning</h3><p>{content.application}</p>
    </section>
    <section className="summary-card">
      <h2>What I can now explain</h2>
      <ul>{content.recap.map(point=><li key={point}>{point}</li>)}</ul>
    </section>
    {isStaff?<section className="summary-card">
      <h2>Additional teacher answer guide</h2>
      <p className="privacy-note">Visible only to authenticated teaching staff. Use professional judgement for valid equivalent responses and local language context.</p>
      <h3>Guided practice</h3>
      <ol>{content.guided.map(([q,,a],i)=><li key={i}><p>{q}</p><strong>Expected response: </strong>{a}</li>)}</ol>
      <h3>Independent practice</h3>
      <ol>{content.practice.map(([q,a],i)=><li key={i}><p>{q}</p><strong>Expected response: </strong>{a}</li>)}</ol>
    </section>:null}
  </div>;
}
