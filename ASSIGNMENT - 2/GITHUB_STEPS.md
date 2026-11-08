# GitHub submission steps (commit progressively – one commit per task)

1. Create an EMPTY repository on GitHub named `CSA09-<RegisterNumber>-Assignment2`
   (public, or private and add the faculty as a collaborator).
2. On your computer:
```
git init
git branch -M main
git remote add origin https://github.com/<username>/CSA09-<RegisterNumber>-Assignment2.git
```
3. Do the tasks on different days and commit each one when you have RUN it yourself:
```
git add Q1 && git commit -m "Q1: String vs StringBuilder vs StringBuffer experiments"
git add Q2 && git commit -m "Q2: ArrayList / LinkedList / ArrayDeque experiments"
git add Q3 && git commit -m "Q3: hashCode-equals contract tests and completed Attendance"
git add Q4 && git commit -m "Q4: HashMap / TreeMap / LinkedHashMap pharmacy inventory"
git add Q5 && git commit -m "Q5: generic Leaderboard and comparator design"
git add Q6 && git commit -m "Q6: triage PriorityQueue, starvation and fair rule"
git add Q7 && git commit -m "Q7: redesigned fair triage and validation"
git add README.md graphs scripts report && git commit -m "Add README, graphs and report"
git push -u origin main
```
4. Paste the repository link on the cover page and in the assignment sheet.
