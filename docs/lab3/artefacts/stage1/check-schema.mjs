// Validates the Stage 1 JSON Schema, and asserts that the conditional rule
// actually accepts and rejects the payloads the spec says it should.
// The schema is read out of the committed Artifact A, not retyped here, so
// this checks the delivered artefact rather than a copy of it.
import { readFileSync } from "node:fs";
import Ajv2020 from "ajv/dist/2020.js";
import addFormats from "ajv-formats";

const SPEC = process.argv[2];
const md = readFileSync(SPEC, "utf8");

const fence = md.match(/```json\n([\s\S]*?)```/);
if (!fence) {
  console.error("FAIL: no ```json block found in " + SPEC);
  process.exit(1);
}
const schema = JSON.parse(fence[1]);
console.log(`Schema extracted from: ${SPEC}`);
console.log(`  title   = ${schema.title}`);
console.log(`  $schema = ${schema.$schema}\n`);

const ajv = new Ajv2020({ allErrors: true, strict: true });
addFormats(ajv);

let validate;
try {
  validate = ajv.compile(schema);
  console.log("Schema compiles under draft 2020-12 in strict mode: PASS\n");
} catch (e) {
  console.error("Schema failed to compile: " + e.message);
  process.exit(1);
}

// Each case states what the spec claims, so a failure names the broken rule.
const cases = [
  ["name only", { name: "Revise maths" }, true, "due date is optional"],
  ["name + dueDate", { name: "Essay", dueDate: "2026-09-30" }, true, "due date accepted"],
  ["name + dueDate + reminder", { name: "Essay", dueDate: "2026-09-30", remindBeforeDays: 3 }, true, "reminder with a due date is valid"],
  ["reminder WITHOUT dueDate", { name: "Essay", remindBeforeDays: 3 }, false, "THE if/then RULE: reminder requires a due date"],
  ["reminder with null dueDate", { name: "Essay", dueDate: null, remindBeforeDays: 3 }, false, "if/then rule holds against explicit null too"],
  ["dueDate null, no reminder", { name: "Essay", dueDate: null }, true, "explicit null means no due date"],
  ["reminder = 0 boundary", { name: "Essay", dueDate: "2026-09-30", remindBeforeDays: 0 }, true, "lower bound inclusive"],
  ["reminder = 30 boundary", { name: "Essay", dueDate: "2026-09-30", remindBeforeDays: 30 }, true, "upper bound inclusive"],
  ["reminder = -1", { name: "Essay", dueDate: "2026-09-30", remindBeforeDays: -1 }, false, "below minimum"],
  ["reminder = 31", { name: "Essay", dueDate: "2026-09-30", remindBeforeDays: 31 }, false, "above maximum"],
  ["empty name", { name: "" }, false, "minLength 1"],
  ["name 121 chars", { name: "x".repeat(121) }, false, "maxLength 120"],
  ["name 120 chars", { name: "x".repeat(120) }, true, "maxLength boundary inclusive"],
  ["missing name", { dueDate: "2026-09-30" }, false, "name is required"],
  ["bad date format", { name: "Essay", dueDate: "30/09/2026" }, false, "format: date"],
  ["unknown property", { name: "Essay", nickname: "x" }, false, "additionalProperties false"],
  ["reminder as string", { name: "Essay", dueDate: "2026-09-30", remindBeforeDays: "3" }, false, "integer type enforced"],
];

let pass = 0, fail = 0;
for (const [label, payload, shouldBeValid, why] of cases) {
  const ok = validate(payload);
  const correct = ok === shouldBeValid;
  if (correct) pass++; else fail++;
  const verdict = correct ? "PASS" : "FAIL";
  const expected = shouldBeValid ? "valid" : "invalid";
  console.log(`[${verdict}] ${label.padEnd(28)} expected ${expected.padEnd(7)} -> ${why}`);
  if (!correct) {
    console.log(`         got ${ok ? "valid" : "invalid"}; errors: ${JSON.stringify(validate.errors)}`);
  }
}

console.log(`\n${pass} passed, ${fail} failed, ${cases.length} total`);
process.exit(fail === 0 ? 0 : 1);
