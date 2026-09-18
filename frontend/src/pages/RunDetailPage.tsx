import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api, HttpError } from "../api/client";
import type {
  DiscrepancyResponse,
  ReconciliationResultResponse,
  ReconciliationResultStatus,
  ReconciliationRunResponse,
} from "../types";
import { StatusBadge } from "../components/StatusBadge";

const ALL_STATUSES: ReconciliationResultStatus[] = ["MATCHED", "UNDERPAID", "OVERPAID", "MISSING", "UNEXPLAINED"];

export function RunDetailPage() {
  const { runId } = useParams<{ runId: string }>();
  const [run, setRun] = useState<ReconciliationRunResponse | null>(null);
  const [results, setResults] = useState<ReconciliationResultResponse[]>([]);
  const [statusFilter, setStatusFilter] = useState<ReconciliationResultStatus | "">("");
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [discrepancies, setDiscrepancies] = useState<DiscrepancyResponse[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!runId) return;
    api
      .get<ReconciliationRunResponse>(`/api/reconciliation/runs/${runId}`)
      .then(setRun)
      .catch((err) => setError(err instanceof HttpError ? err.message : "Failed to load run"));
  }, [runId]);

  useEffect(() => {
    if (!runId) return;
    api
      .get<ReconciliationResultResponse[]>("/api/reconciliation/results", {
        runId,
        status: statusFilter || undefined,
      })
      .then(setResults)
      .catch((err) => setError(err instanceof HttpError ? err.message : "Failed to load results"));
  }, [runId, statusFilter]);

  async function toggleExpand(resultId: string) {
    if (expandedId === resultId) {
      setExpandedId(null);
      return;
    }
    setExpandedId(resultId);
    try {
      const rows = await api.get<DiscrepancyResponse[]>(`/api/reconciliation/results/${resultId}/discrepancies`);
      setDiscrepancies(rows);
    } catch (err) {
      setError(err instanceof HttpError ? err.message : "Failed to load discrepancies");
    }
  }

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
      <Link to="/reconciliation">← Back to reconciliation</Link>
      {error && <p style={{ color: "#cf222e" }}>{error}</p>}

      {run && (
        <section>
          <h1 style={{ fontSize: 20, marginBottom: 4 }}>
            {run.platformDisplayName} — {run.periodStart} → {run.periodEnd}
          </h1>
          <p style={{ color: "#57606a", margin: 0 }}>
            Executed {new Date(run.executedAt).toLocaleString()} · {run.totalResults} results
          </p>
        </section>
      )}

      <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
        <span style={{ fontSize: 13, fontWeight: 600 }}>Filter by status:</span>
        <button onClick={() => setStatusFilter("")} disabled={statusFilter === ""}>
          All
        </button>
        {ALL_STATUSES.map((status) => (
          <button key={status} onClick={() => setStatusFilter(status)} disabled={statusFilter === status}>
            {status}
          </button>
        ))}
      </div>

      <table style={{ width: "100%", borderCollapse: "collapse", backgroundColor: "#fff", border: "1px solid #d0d7de" }}>
        <thead>
          <tr style={{ textAlign: "left", borderBottom: "1px solid #d0d7de" }}>
            <th style={{ padding: 8 }}>Order</th>
            <th style={{ padding: 8 }}>Expected</th>
            <th style={{ padding: 8 }}>Actual</th>
            <th style={{ padding: 8 }}>Difference</th>
            <th style={{ padding: 8 }}>Status</th>
            <th style={{ padding: 8 }}>Explanation</th>
          </tr>
        </thead>
        <tbody>
          {results.length === 0 && (
            <tr>
              <td colSpan={6} style={{ padding: 12, color: "#57606a" }}>
                No results for this filter.
              </td>
            </tr>
          )}
          {results.map((result) => (
            <>
              <tr
                key={result.id}
                onClick={() => toggleExpand(result.id)}
                style={{ borderTop: "1px solid #eaeef2", cursor: "pointer" }}
              >
                <td style={{ padding: 8 }}>{result.platformOrderId ?? "—"}</td>
                <td style={{ padding: 8 }}>{result.expectedAmount ?? "—"}</td>
                <td style={{ padding: 8 }}>{result.actualAmount ?? "—"}</td>
                <td style={{ padding: 8 }}>{result.difference ?? "—"}</td>
                <td style={{ padding: 8 }}>
                  <StatusBadge status={result.status} />
                </td>
                <td style={{ padding: 8, fontSize: 13, color: "#57606a" }}>{result.explanation}</td>
              </tr>
              {expandedId === result.id && (
                <tr key={`${result.id}-detail`}>
                  <td colSpan={6} style={{ padding: 12, backgroundColor: "#f6f8fa" }}>
                    {discrepancies.length === 0 ? (
                      <span style={{ color: "#57606a", fontSize: 13 }}>No discrepancy breakdown for this result.</span>
                    ) : (
                      <table style={{ width: "100%", borderCollapse: "collapse" }}>
                        <thead>
                          <tr style={{ textAlign: "left", fontSize: 12, color: "#57606a" }}>
                            <th style={{ padding: 4 }}>Category</th>
                            <th style={{ padding: 4 }}>Expected</th>
                            <th style={{ padding: 4 }}>Actual</th>
                            <th style={{ padding: 4 }}>Difference</th>
                            <th style={{ padding: 4 }}>Notes</th>
                          </tr>
                        </thead>
                        <tbody>
                          {discrepancies.map((d) => (
                            <tr key={d.id} style={{ fontSize: 13 }}>
                              <td style={{ padding: 4 }}>{d.category}</td>
                              <td style={{ padding: 4 }}>{d.expectedAmount}</td>
                              <td style={{ padding: 4 }}>{d.actualAmount}</td>
                              <td style={{ padding: 4 }}>{d.differenceAmount}</td>
                              <td style={{ padding: 4 }}>{d.notes}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    )}
                  </td>
                </tr>
              )}
            </>
          ))}
        </tbody>
      </table>
    </div>
  );
}
