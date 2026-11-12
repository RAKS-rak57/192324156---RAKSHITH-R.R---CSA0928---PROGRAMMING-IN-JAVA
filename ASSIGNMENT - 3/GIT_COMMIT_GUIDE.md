# Progressive commits (required: at least one commit per question; both students must do this in their OWN repository)

```
git init && git branch -M main
git remote add origin https://github.com/<user>/CSA09-<RegisterNumber>-Assignment3.git
git add README.md .gitignore run_all.sh docs && git commit -m "README, run script, diagrams"
git add Q1 && git commit -m "Q1: ATM exception handling"
git add Q2 && git commit -m "Q2: portal exception hierarchy and chaining"
git add Q3 && git commit -m "Q3: resource safety, try-with-resources, debugging"
git add Q4 && git commit -m "Q4: threads and life cycle"
git add Q5 && git commit -m "Q5: race condition and synchronization"
git add Q6 && git commit -m "Q6: deadlock and producer-consumer"
git add Q7 && git commit -m "Q7: concurrent result processor"
git push -u origin main
```
Make the commits on different days as you finish each question. Retake the screenshots on your own computer, and re-run the
timing experiments there (the numbers in the report come from a single-core machine). Do not commit `Q7/data` (it is generated).
