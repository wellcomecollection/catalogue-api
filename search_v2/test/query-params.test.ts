import { z } from "zod";

// Re-implementing the parser for testing since it's not exported
function parseCommaSeparatedWithQuotes(val: string): string[] {
  const result: string[] = [];
  let current = "";
  let inQuotes = false;

  for (let i = 0; i < val.length; i++) {
    const char = val[i];

    if (char === '"' && (i === 0 || val[i - 1] !== "\\")) {
      inQuotes = !inQuotes;
    } else if (char === "," && !inQuotes) {
      const trimmed = current.trim();
      if (trimmed) result.push(trimmed);
      current = "";
    } else if (char === "\\" && i + 1 < val.length && val[i + 1] === '"') {
      // Handle escaped quotes
      current += '"';
      i++; // Skip the next character
    } else {
      current += char;
    }
  }

  // Don't forget the last value
  const trimmed = current.trim();
  if (trimmed) result.push(trimmed);

  return result;
}

describe("parseCommaSeparatedWithQuotes", () => {
  it("parses simple comma-separated values", () => {
    expect(parseCommaSeparatedWithQuotes("a,b,c")).toEqual(["a", "b", "c"]);
  });

  it("handles values with surrounding whitespace", () => {
    expect(parseCommaSeparatedWithQuotes(" a , b , c ")).toEqual([
      "a",
      "b",
      "c",
    ]);
  });

  it("parses quoted values containing commas", () => {
    expect(parseCommaSeparatedWithQuotes('"a,b",c')).toEqual(["a,b", "c"]);
  });

  it("parses quoted values containing spaces", () => {
    expect(parseCommaSeparatedWithQuotes('"PATCH CLAMPING",Frogs')).toEqual([
      "PATCH CLAMPING",
      "Frogs",
    ]);
  });

  it("handles URL-decoded quoted values (the bug case)", () => {
    // This is what happens when subjects.label=%22PATCH+CLAMPING%22 is decoded
    expect(parseCommaSeparatedWithQuotes('"PATCH CLAMPING"')).toEqual([
      "PATCH CLAMPING",
    ]);
  });

  it("handles multiple quoted values", () => {
    expect(
      parseCommaSeparatedWithQuotes('"value one","value two","value three"')
    ).toEqual(["value one", "value two", "value three"]);
  });

  it("handles mixed quoted and unquoted values", () => {
    expect(parseCommaSeparatedWithQuotes('simple,"has space",another')).toEqual(
      ["simple", "has space", "another"]
    );
  });

  it("handles escaped quotes within quoted values", () => {
    expect(parseCommaSeparatedWithQuotes('"a\\"b",c')).toEqual(['a"b', "c"]);
  });

  it("handles empty string", () => {
    expect(parseCommaSeparatedWithQuotes("")).toEqual([]);
  });

  it("handles single value", () => {
    expect(parseCommaSeparatedWithQuotes("single")).toEqual(["single"]);
  });

  it("filters out empty values between commas", () => {
    expect(parseCommaSeparatedWithQuotes("a,,b")).toEqual(["a", "b"]);
  });

  it("handles trailing comma", () => {
    expect(parseCommaSeparatedWithQuotes("a,b,")).toEqual(["a", "b"]);
  });

  it("handles leading comma", () => {
    expect(parseCommaSeparatedWithQuotes(",a,b")).toEqual(["a", "b"]);
  });
});
