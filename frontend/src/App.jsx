import { useState } from "react";
import { Routes, Route, Navigate } from "react-router-dom";
import LoginPage from "./LoginPage";
import TestPage from "./TestPage";
import MyRequestPage from "./features/maintenance/MyRequestPage.jsx";
import NewRequestPage from "./features/maintenance/NewRequestPage.jsx";

function App() {
  const [token, setToken] = useState(() => localStorage.getItem("token"));

  function handleLogin(newToken) {
    localStorage.setItem("token", newToken);
    setToken(newToken);
  }

  function handleLogout() {
    localStorage.removeItem("token");
    setToken(null);
  }

  if (!token) {
    return <LoginPage onLogin={handleLogin}/>;
  }

  return (
      <Routes>
        <Route path="/" element={<TestPage token={token} onLogout={handleLogout} />} />
        <Route path="/maintenance" element={<MyRequestPage token={token} />} />
        <Route path="/maintenance/new" element={<NewRequestPage token={token} />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
  )
}

export default App;
