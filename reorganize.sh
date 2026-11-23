#!/usr/bin/env bash
# Run this ONCE from the root of your ShareShelf repo (the folder that contains README.md and .git).
# It only MOVES files with git, so history is kept. Then unzip ShareShelf-repo-cleanup.zip over the folder.
set -e
test -d .git || { echo "Run this from the repo root (the folder with .git)"; exit 1; }
mkdir -p database/migrations docs/guides docs/design docs/planning docs/testing/test-results docs/team

[ -e "docs/SETUP_GUIDE.md" ] && git mv "docs/SETUP_GUIDE.md" "docs/guides/SETUP_GUIDE.md" || echo "skipped (not found): docs/SETUP_GUIDE.md"
[ -e "docs/DEPLOYMENT.md" ] && git mv "docs/DEPLOYMENT.md" "docs/guides/DEPLOYMENT.md" || echo "skipped (not found): docs/DEPLOYMENT.md"
[ -e "docs/BUILD_IT_YOURSELF.md" ] && git mv "docs/BUILD_IT_YOURSELF.md" "docs/guides/BUILD_IT_YOURSELF.md" || echo "skipped (not found): docs/BUILD_IT_YOURSELF.md"
[ -e "docs/USER_FLOWS.md" ] && git mv "docs/USER_FLOWS.md" "docs/design/USER_FLOWS.md" || echo "skipped (not found): docs/USER_FLOWS.md"
[ -e "docs/UI_GUIDE.md" ] && git mv "docs/UI_GUIDE.md" "docs/design/UI_GUIDE.md" || echo "skipped (not found): docs/UI_GUIDE.md"
[ -e "docs/DAO_CONTRACT.md" ] && git mv "docs/DAO_CONTRACT.md" "docs/design/DAO_CONTRACT.md" || echo "skipped (not found): docs/DAO_CONTRACT.md"
[ -e "docs/FOLDER_STRUCTURE.md" ] && git mv "docs/FOLDER_STRUCTURE.md" "docs/design/FOLDER_STRUCTURE.md" || echo "skipped (not found): docs/FOLDER_STRUCTURE.md"
[ -e "docs/PROJECT_SUMMARY.md" ] && git mv "docs/PROJECT_SUMMARY.md" "docs/planning/PROJECT_SUMMARY.md" || echo "skipped (not found): docs/PROJECT_SUMMARY.md"
[ -e "docs/FINAL_SPEC.md" ] && git mv "docs/FINAL_SPEC.md" "docs/planning/FINAL_SPEC.md" || echo "skipped (not found): docs/FINAL_SPEC.md"
[ -e "docs/ROADMAP.md" ] && git mv "docs/ROADMAP.md" "docs/planning/ROADMAP.md" || echo "skipped (not found): docs/ROADMAP.md"
[ -e "docs/REVIEW_0.md" ] && git mv "docs/REVIEW_0.md" "docs/planning/REVIEW_0.md" || echo "skipped (not found): docs/REVIEW_0.md"
[ -e "docs/TESTING.md" ] && git mv "docs/TESTING.md" "docs/testing/TESTING.md" || echo "skipped (not found): docs/TESTING.md"
[ -e "docs/HOW_TO_TEST.md" ] && git mv "docs/HOW_TO_TEST.md" "docs/testing/HOW_TO_TEST.md" || echo "skipped (not found): docs/HOW_TO_TEST.md"
[ -e "docs/TEST_CASES.md" ] && git mv "docs/TEST_CASES.md" "docs/testing/TEST_CASES.md" || echo "skipped (not found): docs/TEST_CASES.md"
[ -e "docs/TEAM_ROLES.md" ] && git mv "docs/TEAM_ROLES.md" "docs/team/TEAM_ROLES.md" || echo "skipped (not found): docs/TEAM_ROLES.md"
[ -e "docs/TEAM_CONTRIBUTIONS.md" ] && git mv "docs/TEAM_CONTRIBUTIONS.md" "docs/team/TEAM_CONTRIBUTIONS.md" || echo "skipped (not found): docs/TEAM_CONTRIBUTIONS.md"
[ -e "docs/migration-2026-11-18.sql" ] && git mv "docs/migration-2026-11-18.sql" "database/migrations/001_due-dates-and-meet-time.sql" || echo "skipped (not found): docs/migration-2026-11-18.sql"
[ -e "docs/migration-2026-11-19.sql" ] && git mv "docs/migration-2026-11-19.sql" "database/migrations/002_handover-tokens.sql" || echo "skipped (not found): docs/migration-2026-11-19.sql"
[ -e "docs/migration-2026-11-19b.sql" ] && git mv "docs/migration-2026-11-19b.sql" "database/migrations/003_borrow-hours-and-return-condition.sql" || echo "skipped (not found): docs/migration-2026-11-19b.sql"
[ -e "docs/migration-2026-11-19c.sql" ] && git mv "docs/migration-2026-11-19c.sql" "database/migrations/004_accepted-time-and-wanted-board.sql" || echo "skipped (not found): docs/migration-2026-11-19c.sql"
[ -e "docs/migration-2026-11-20.sql" ] && git mv "docs/migration-2026-11-20.sql" "database/migrations/005_wanted-location.sql" || echo "skipped (not found): docs/migration-2026-11-20.sql"
[ -e "docs/migration-2026-11-21.sql" ] && git mv "docs/migration-2026-11-21.sql" "database/migrations/006_remember-me-tokens.sql" || echo "skipped (not found): docs/migration-2026-11-21.sql"
[ -e "schema.sql" ] && git mv "schema.sql" "database/schema.sql" || echo "skipped (not found): schema.sql"
[ -e "schema-hosted.sql" ] && git mv "schema-hosted.sql" "database/schema-hosted.sql" || echo "skipped (not found): schema-hosted.sql"
[ -e "docs/test-results/unit-output.txt" ] && git mv "docs/test-results/unit-output.txt" "docs/testing/test-results/unit-output.txt" || echo "skipped (not found): docs/test-results/unit-output.txt"
[ -e "docs/test-results/e2e-output.txt" ] && git mv "docs/test-results/e2e-output.txt" "docs/testing/test-results/e2e-output.txt" || echo "skipped (not found): docs/test-results/e2e-output.txt"

rmdir docs/test-results 2>/dev/null || true
echo "Done moving. Now unzip ShareShelf-repo-cleanup.zip over this folder, then:"
echo "  git add -A && git commit -m 'Reorganise repo: database/ and docs/ folders, new README' && git push"
