import { Routes, Route, Navigate } from "react-router-dom";
import { RequireAuth } from "./components/Layout";
import { LoginPage } from "./pages/LoginPage";
import { RegisterPage } from "./pages/RegisterPage";
import { DashboardPage } from "./pages/DashboardPage";
import { UploadsPage } from "./pages/UploadsPage";
import { ReconciliationPage } from "./pages/ReconciliationPage";
import { RunDetailPage } from "./pages/RunDetailPage";

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route element={<RequireAuth />}>
        <Route path="/" element={<DashboardPage />} />
        <Route path="/uploads" element={<UploadsPage />} />
        <Route path="/reconciliation" element={<ReconciliationPage />} />
        <Route path="/reconciliation/:runId" element={<RunDetailPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
