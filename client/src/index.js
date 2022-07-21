import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import App from './App';
import * as cssVars from "./theme.js";

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
    // <React.StrictMode>
        <App/>
    // </React.StrictMode>
);

window.onload = async () => {
    await cssVars.initCssVars();
}
