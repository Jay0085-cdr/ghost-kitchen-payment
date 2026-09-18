import { NavLink, Outlet, Navigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

export function Layout() {
  const { user, logout } = useAuth();

  const linkStyle = ({ isActive }: { isActive: boolean }) => ({
    padding: "8px 14px",
    borderRadius: 6,
    textDecoration: "none",
    color: isActive ? "#fff" : "#24292f",
    backgroundColor: isActive ? "#1f2937" : "transparent",
    fontWeight: 500,
    fontSize: 14,
  });

  return (
    <div style={{ fontFamily: "system-ui, sans-serif", minHeight: "100vh", backgroundColor: "#f6f8fa" }}>
      <header
        style={{
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          padding: "12px 24px",
          backgroundColor: "#fff",
          borderBottom: "1px solid #d0d7de",
        }}
      >
        <div style={{ display: "flex", alignItems: "center", gap: 24 }}>
          <strong style={{ fontSize: 16 }}>Ghost Kitchen</strong>
          <nav style={{ display: "flex", gap: 4 }}>
            <NavLink to="/" end style={linkStyle}>Dashboard</NavLink>
            <NavLink to="/uploads" style={linkStyle}>Uploads</NavLink>
            <NavLink to="/reconciliation" style={linkStyle}>Reconciliation</NavLink>
          </nav>
        </div>
        <div style={{ display: "flex", alignItems: "center", gap: 12, fontSize: 14, color: "#57606a" }}>
          <span>{user?.email}</span>
          <button onClick={logout} style={{ cursor: "pointer" }}>
            Log out
          </button>
        </div>
      </header>
      <main style={{ maxWidth: 1000, margin: "0 auto", padding: "24px" }}>
        <Outlet />
      </main>
    </div>
  );
}

export function RequireAuth() {
  const { user, loading } = useAuth();
  if (loading) return null;
  if (!user) return <Navigate to="/login" replace />;
  return <Layout />;
}
