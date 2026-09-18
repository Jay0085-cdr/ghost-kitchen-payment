import type { ReconciliationResultStatus, ReportStatus } from "../types";

const COLORS: Record<string, string> = {
  MATCHED: "#1a7f37",
  PARSED: "#1a7f37",
  UNDERPAID: "#b35900",
  OVERPAID: "#9a6700",
  MISSING: "#cf222e",
  UNEXPLAINED: "#8250df",
  FAILED: "#cf222e",
  PENDING: "#57606a",
  RUNNING: "#57606a",
  COMPLETED: "#1a7f37",
};

export function StatusBadge({ status }: { status: ReconciliationResultStatus | ReportStatus | string }) {
  const color = COLORS[status] ?? "#57606a";
  return (
    <span
      style={{
        display: "inline-block",
        padding: "2px 10px",
        borderRadius: 999,
        fontSize: 12,
        fontWeight: 600,
        color: "#fff",
        backgroundColor: color,
        whiteSpace: "nowrap",
      }}
    >
      {status}
    </span>
  );
}
