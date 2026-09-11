import { useState } from "react";
import LoginPage from "./LoginPage";
import TestPage from "./TestPage";

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

  return token ? (
    <TestPage token={token} onLogout={handleLogout} />
  ) : (
    <LoginPage onLogin={handleLogin} />
  );
}

export default App;
