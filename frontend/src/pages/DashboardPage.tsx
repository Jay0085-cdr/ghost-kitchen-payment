import { useEffect, useState } from "react";
import { api, HttpError } from "../api/client";
import type { OrganizationResponse, ReconciliationResultStatus, ReconciliationRunResponse } from "../types";
import { useAuth } from "../auth/AuthContext";
import { StatusBadge } from "../components/StatusBadge";

export function DashboardPage() {
  const { user } = useAuth();
  const [organization, setOrganization] = useState<OrganizationResponse | null>(null);
  const [runs, setRuns] = useState<ReconciliationRunResponse[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!user) return;
    Promise.all([
      api.get<OrganizationResponse>(`/api/organizations/${user.organizationId}`),
      api.get<ReconciliationRunResponse[]>("/api/reconciliation/runs"),
    ])
      .then(([org, runList]) => {
        setOrganization(org);
        setRuns(runList);
      })
      .catch((err) => setError(err instanceof HttpError ? err.message : "Failed to load dashboard"))
      .finally(() => setLoading(false));
  }, [user]);

  const totals: Partial<Record<ReconciliationResultStatus, number>> = {};
  for (const run of runs) {
    for (const [status, count] of Object.entries(run.statusCounts)) {
      const key = status as ReconciliationResultStatus;
      totals[key] = (totals[key] ?? 0) + (count ?? 0);
    }
  }

  if (loading) return <p>Loading...</p>;
  if (error) return <p style={{ color: "#cf222e" }}>{error}</p>;

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 24 }}>
      <section>
        <h1 style={{ marginBottom: 4 }}>{organization?.name}</h1>
        <p style={{ color: "#57606a", margin: 0 }}>
          Signed in as {user?.email} ({user?.role})
        </p>
      </section>

      <section>
        <h2 style={{ fontSize: 16 }}>Summary — all reconciliation runs</h2>
        {runs.length === 0 ? (
          <p style={{ color: "#57606a" }}>
            No reconciliation runs yet. Upload a settlement report and a bank statement, then trigger a run from the
            Reconciliation page.
          </p>
        ) : (
          <div style={{ display: "flex", gap: 12, flexWrap: "wrap" }}>
            {(["MATCHED", "UNDERPAID", "OVERPAID", "MISSING", "UNEXPLAINED"] as const).map((status) => (
              <div
                key={status}
                style={{
                  padding: "12px 18px",
                  backgroundColor: "#fff",
                  border: "1px solid #d0d7de",
                  borderRadius: 8,
                  minWidth: 120,
                }}
              >
                <div style={{ fontSize: 24, fontWeight: 700 }}>{totals[status] ?? 0}</div>
                <StatusBadge status={status} />
              </div>
            ))}
          </div>
        )}
      </section>

      <section>
        <h2 style={{ fontSize: 16 }}>Recent runs</h2>
        {runs.length === 0 ? (
          <p style={{ color: "#57606a" }}>Nothing yet.</p>
        ) : (
          <table style={{ width: "100%", borderCollapse: "collapse" }}>
            <thead>
              <tr style={{ textAlign: "left", borderBottom: "1px solid #d0d7de" }}>
                <th style={{ padding: 8 }}>Platform</th>
                <th style={{ padding: 8 }}>Period</th>
                <th style={{ padding: 8 }}>Executed</th>
                <th style={{ padding: 8 }}>Results</th>
                <th style={{ padding: 8 }}>Status</th>
              </tr>
            </thead>
            <tbody>
              {runs.slice(0, 10).map((run) => (
                <tr key={run.id} style={{ borderBottom: "1px solid #eaeef2" }}>
                  <td style={{ padding: 8 }}>{run.platformDisplayName}</td>
                  <td style={{ padding: 8 }}>
                    {run.periodStart} → {run.periodEnd}
                  </td>
                  <td style={{ padding: 8 }}>{new Date(run.executedAt).toLocaleString()}</td>
                  <td style={{ padding: 8 }}>{run.totalResults}</td>
                  <td style={{ padding: 8 }}>
                    <StatusBadge status={run.status} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </div>
  );
}
