import { useEffect, useState, type FormEvent } from "react";
import { api, HttpError } from "../api/client";
import type { BankStatementResponse, PlatformResponse, SettlementReportResponse } from "../types";
import { StatusBadge } from "../components/StatusBadge";

const cardStyle = {
  backgroundColor: "#fff",
  border: "1px solid #d0d7de",
  borderRadius: 8,
  padding: 16,
};

export function UploadsPage() {
  const [platforms, setPlatforms] = useState<PlatformResponse[]>([]);
  const [reports, setReports] = useState<SettlementReportResponse[]>([]);
  const [statements, setStatements] = useState<BankStatementResponse[]>([]);
  const [error, setError] = useState<string | null>(null);

  function refresh() {
    Promise.all([
      api.get<PlatformResponse[]>("/api/platforms"),
      api.get<SettlementReportResponse[]>("/api/settlement-reports"),
      api.get<BankStatementResponse[]>("/api/bank-statements"),
    ])
      .then(([p, r, b]) => {
        setPlatforms(p);
        setReports(r);
        setStatements(b);
      })
      .catch((err) => setError(err instanceof HttpError ? err.message : "Failed to load uploads"));
  }

  useEffect(refresh, []);

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 24 }}>
      {error && <p style={{ color: "#cf222e" }}>{error}</p>}

      <SettlementReportUploadForm platforms={platforms} onUploaded={refresh} />
      <BankStatementUploadForm onUploaded={refresh} />

      <section>
        <h2 style={{ fontSize: 16 }}>Settlement reports</h2>
        <div style={cardStyle}>
          {reports.length === 0 ? (
            <p style={{ color: "#57606a", margin: 0 }}>No settlement reports uploaded yet.</p>
          ) : (
            <table style={{ width: "100%", borderCollapse: "collapse" }}>
              <thead>
                <tr style={{ textAlign: "left" }}>
                  <th style={{ padding: 6 }}>File</th>
                  <th style={{ padding: 6 }}>Platform</th>
                  <th style={{ padding: 6 }}>Period</th>
                  <th style={{ padding: 6 }}>Transactions</th>
                  <th style={{ padding: 6 }}>Status</th>
                </tr>
              </thead>
              <tbody>
                {reports.map((r) => (
                  <tr key={r.id} style={{ borderTop: "1px solid #eaeef2" }}>
                    <td style={{ padding: 6 }}>{r.fileName}</td>
                    <td style={{ padding: 6 }}>{r.platformDisplayName}</td>
                    <td style={{ padding: 6 }}>
                      {r.periodStart} → {r.periodEnd}
                    </td>
                    <td style={{ padding: 6 }}>{r.transactionCount}</td>
                    <td style={{ padding: 6 }}>
                      <StatusBadge status={r.status} />
                      {r.status === "FAILED" && r.errorDetail && (
                        <div style={{ fontSize: 12, color: "#cf222e", marginTop: 4 }}>{r.errorDetail}</div>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </section>

      <section>
        <h2 style={{ fontSize: 16 }}>Bank statements</h2>
        <div style={cardStyle}>
          {statements.length === 0 ? (
            <p style={{ color: "#57606a", margin: 0 }}>No bank statements uploaded yet.</p>
          ) : (
            <table style={{ width: "100%", borderCollapse: "collapse" }}>
              <thead>
                <tr style={{ textAlign: "left" }}>
                  <th style={{ padding: 6 }}>File</th>
                  <th style={{ padding: 6 }}>Uploaded</th>
                  <th style={{ padding: 6 }}>Status</th>
                </tr>
              </thead>
              <tbody>
                {statements.map((s) => (
                  <tr key={s.id} style={{ borderTop: "1px solid #eaeef2" }}>
                    <td style={{ padding: 6 }}>{s.fileName}</td>
                    <td style={{ padding: 6 }}>{new Date(s.uploadedAt).toLocaleString()}</td>
                    <td style={{ padding: 6 }}>
                      <StatusBadge status={s.status} />
                      {s.status === "FAILED" && s.errorDetail && (
                        <div style={{ fontSize: 12, color: "#cf222e", marginTop: 4 }}>{s.errorDetail}</div>
                      )}
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

function SettlementReportUploadForm({
  platforms,
  onUploaded,
}: {
  platforms: PlatformResponse[];
  onUploaded: () => void;
}) {
  const [platformId, setPlatformId] = useState("");
  const [periodStart, setPeriodStart] = useState("");
  const [periodEnd, setPeriodEnd] = useState("");
  const [file, setFile] = useState<File | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!file) return;
    setError(null);
    setSubmitting(true);
    try {
      const form = new FormData();
      form.append("file", file);
      form.append("platformId", platformId);
      form.append("periodStart", periodStart);
      form.append("periodEnd", periodEnd);
      await api.postForm("/api/settlement-reports/upload", form);
      setFile(null);
      onUploaded();
    } catch (err) {
      setError(err instanceof HttpError ? err.message : "Upload failed");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section>
      <h2 style={{ fontSize: 16 }}>Upload settlement report</h2>
      <form onSubmit={handleSubmit} style={{ ...cardStyle, display: "flex", gap: 12, flexWrap: "wrap", alignItems: "end" }}>
        <Field label="Platform">
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
        </Field>
        <Field label="Period start">
          <input type="date" value={periodStart} onChange={(e) => setPeriodStart(e.target.value)} required />
        </Field>
        <Field label="Period end">
          <input type="date" value={periodEnd} onChange={(e) => setPeriodEnd(e.target.value)} required />
        </Field>
        <Field label="CSV file">
          <input type="file" accept=".csv" onChange={(e) => setFile(e.target.files?.[0] ?? null)} required />
        </Field>
        <button type="submit" disabled={submitting}>
          {submitting ? "Uploading..." : "Upload"}
        </button>
        {error && <p style={{ color: "#cf222e", width: "100%", margin: 0 }}>{error}</p>}
      </form>
    </section>
  );
}

function BankStatementUploadForm({ onUploaded }: { onUploaded: () => void }) {
  const [file, setFile] = useState<File | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!file) return;
    setError(null);
    setSubmitting(true);
    try {
      const form = new FormData();
      form.append("file", file);
      await api.postForm("/api/bank-statements/upload", form);
      setFile(null);
      onUploaded();
    } catch (err) {
      setError(err instanceof HttpError ? err.message : "Upload failed");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section>
      <h2 style={{ fontSize: 16 }}>Upload bank statement</h2>
      <form onSubmit={handleSubmit} style={{ ...cardStyle, display: "flex", gap: 12, alignItems: "end" }}>
        <Field label="CSV file">
          <input type="file" accept=".csv" onChange={(e) => setFile(e.target.files?.[0] ?? null)} required />
        </Field>
        <button type="submit" disabled={submitting}>
          {submitting ? "Uploading..." : "Upload"}
        </button>
        {error && <p style={{ color: "#cf222e", margin: 0 }}>{error}</p>}
      </form>
    </section>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label style={{ display: "flex", flexDirection: "column", gap: 4, fontSize: 13, fontWeight: 600 }}>
      {label}
      {children}
    </label>
  );
}
