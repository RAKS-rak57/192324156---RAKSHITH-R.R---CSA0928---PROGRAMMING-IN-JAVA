# How to publish this on GitHub with progressive commits (required by the assignment)

The assignment checks the commit history: "at least one commit for each task; a single upload on the last day will not be accepted".
So do NOT upload everything at once. Work through the tasks over the week, and after you have run each task yourself, commit it.

## One-time set-up
1. On github.com create a repository named exactly `CSA09-<YourRegisterNumber>-Assignment1`
   (public, or private and add the course faculty as a collaborator).
2. Rename the folder `CSA09-REGNO-Assignment1` to the same name and open a terminal inside it:
```
git init
git branch -M main
git remote add origin https://github.com/<your-username>/CSA09-<RegisterNumber>-Assignment1.git
git config user.name  "Your Name"
git config user.email "you@example.com"
```

## One commit per task (run each task first, read the code, then commit)
```
git add README.md .gitignore run_all.sh docs      && git commit -m "Add README, run script and UML diagrams"
git add Task01 && git commit -m "Task 1: problem analysis and objects vs parallel arrays"
git add Task02 && git commit -m "Task 2: data representation and casting"
git add Task03 && git commit -m "Task 3: control logic, ParkingFacility complete-the-code"
git add Task04 && git commit -m "Task 4: classes, constructors, copy constructor"
git add Task05 && git commit -m "Task 5: static members and object life cycle"
git add Task06 && git commit -m "Task 6: inheritance hierarchy"
git add Task07 && git commit -m "Task 7: overriding, final, equals/hashCode"
git add Task08 && git commit -m "Task 8: polymorphism, FeeDemo complete-the-code"
git add Task09 && git commit -m "Task 9: abstraction"
git add Task10 && git commit -m "Task 10: packages and encapsulation"
git add Task11 && git commit -m "Task 11: full implementation and tests"
git add Task12 && git commit -m "Task 12: design challenge"
git push -u origin main
```
Spread these commits over several days (push as you go). Put the repository URL on the cover page of the report and in the submission form.

## Before you submit
* Retake the screenshots on YOUR computer (the PNGs here were rendered from real program output, but your own terminal screenshots are what the faculty expects) – replace the files in each `output/` folder.
* Fill in the yellow placeholders in the report and README (name, register number, link, date).
* Be ready for the viva: you may be asked to explain, run or change any part of the code.
