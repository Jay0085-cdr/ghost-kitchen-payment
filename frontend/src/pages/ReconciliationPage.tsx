import { useEffect, useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { api, HttpError } from "../api/client";
import type { PlatformResponse, ReconciliationRunResponse } from "../types";
import { StatusBadge } from "../components/StatusBadge";

const cardStyle = {
  backgroundColor: "#fff",
  border: "1px solid #d0d7de",
  borderRadius: 8,
  padding: 16,
};

export function ReconciliationPage() {
  const [platforms, setPlatforms] = useState<PlatformResponse[]>([]);
  const [runs, setRuns] = useState<ReconciliationRunResponse[]>([]);
  const [platformId, setPlatformId] = useState("");
  const [periodStart, setPeriodStart] = useState("");
  const [periodEnd, setPeriodEnd] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  function refreshRuns() {
    api
      .get<ReconciliationRunResponse[]>("/api/reconciliation/runs")
      .then(setRuns)
      .catch((err) => setError(err instanceof HttpError ? err.message : "Failed to load runs"));
  }

  useEffect(() => {
    api.get<PlatformResponse[]>("/api/platforms").then(setPlatforms).catch(() => {});
    refreshRuns();
  }, []);

  async function handleTrigger(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await api.post("/api/reconciliation/runs", { platformId, periodStart, periodEnd });
      refreshRuns();
    } catch (err) {
      setError(err instanceof HttpError ? err.message : "Failed to trigger reconciliation");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 24 }}>
      <section>
        <h2 style={{ fontSize: 16 }}>Trigger a reconciliation run</h2>
        <form
          onSubmit={handleTrigger}
          style={{ ...cardStyle, display: "flex", gap: 12, flexWrap: "wrap", alignItems: "end" }}
        >
          <label style={{ display: "flex", flexDirection: "column", gap: 4, fontSize: 13, fontWeight: 600 }}>
            Platform
            <select value={platformId} onChange={(e) => setPlatformId(e.target.value)} required>
              <option value="" disabled>
                Select...
              </option>
              {platforms.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.displayName}
                </option>
              ))}
            </select>
          </label>
          <label style={{ display: "flex", flexDirection: "column", gap: 4, fontSize: 13, fontWeight: 600 }}>
            Period start
            <input type="date" value={periodStart} onChange={(e) => setPeriodStart(e.target.value)} required />
          </label>
          <label style={{ display: "flex", flexDirection: "column", gap: 4, fontSize: 13, fontWeight: 600 }}>
            Period end
            <input type="date" value={periodEnd} onChange={(e) => setPeriodEnd(e.target.value)} required />
          </label>
          <button type="submit" disabled={submitting}>
            {submitting ? "Running..." : "Run reconciliation"}
          </button>
          {error && <p style={{ color: "#cf222e", width: "100%", margin: 0 }}>{error}</p>}
        </form>
      </section>

      <section>
        <h2 style={{ fontSize: 16 }}>Past runs</h2>
        <div style={cardStyle}>
          {runs.length === 0 ? (
            <p style={{ color: "#57606a", margin: 0 }}>No runs yet.</p>
          ) : (
            <table style={{ width: "100%", borderCollapse: "collapse" }}>
              <thead>
                <tr style={{ textAlign: "left" }}>
                  <th style={{ padding: 6 }}>Platform</th>
                  <th style={{ padding: 6 }}>Period</th>
                  <th style={{ padding: 6 }}>Executed</th>
                  <th style={{ padding: 6 }}>Results</th>
                  <th style={{ padding: 6 }}>Status</th>
                  <th style={{ padding: 6 }}></th>
                </tr>
              </thead>
              <tbody>
                {runs.map((run) => (
                  <tr key={run.id} style={{ borderTop: "1px solid #eaeef2" }}>
                    <td style={{ padding: 6 }}>{run.platformDisplayName}</td>
                    <td style={{ padding: 6 }}>
                      {run.periodStart} → {run.periodEnd}
                    </td>
                    <td style={{ padding: 6 }}>{new Date(run.executedAt).toLocaleString()}</td>
                    <td style={{ padding: 6 }}>{run.totalResults}</td>
                    <td style={{ padding: 6 }}>
                      <StatusBadge status={run.status} />
                    </td>
                    <td style={{ padding: 6 }}>
                      <Link to={`/reconciliation/${run.id}`}>View results</Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </section>
    </div>
  );
}
