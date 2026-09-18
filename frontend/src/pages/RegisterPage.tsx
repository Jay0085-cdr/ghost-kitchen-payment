import { useState, type FormEvent } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { HttpError } from "../api/client";
import { authPageStyles as s } from "./authPageStyles";

export function RegisterPage() {
  const { user, register } = useAuth();
  const navigate = useNavigate();
  const [organizationName, setOrganizationName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  if (user) return <Navigate to="/" replace />;

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await register(organizationName, email, password);
      navigate("/");
    } catch (err) {
      setError(err instanceof HttpError ? err.message : "Registration failed");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div style={s.page}>
      <form onSubmit={handleSubmit} style={s.card}>
        <h1 style={s.title}>Ghost Kitchen</h1>
        <p style={s.subtitle}>Register your kitchen — this creates a new organization with you as owner</p>
        {error && <div style={s.error}>{error}</div>}
        <label style={s.label}>
          Kitchen / restaurant name
          <input
            style={s.input}
            value={organizationName}
            onChange={(e) => setOrganizationName(e.target.value)}
            required
          />
        </label>
        <label style={s.label}>
          Email
          <input style={s.input} type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </label>
        <label style={s.label}>
          Password
          <input
            style={s.input}
            type="password"
            minLength={8}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </label>
        <button style={s.button} type="submit" disabled={submitting}>
          {submitting ? "Creating..." : "Create account"}
        </button>
        <p style={s.footer}>
          Already have an account? <Link to="/login">Sign in</Link>
        </p>
      </form>
    </div>
  );
}
