import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import App from './app/App';
import {initCssVars} from "./app/service/theme";

const root = ReactDOM.createRoot(document.getElementById("root") as HTMLElement);
root.render(
    <App/>
);

window.onload = async () => {
    await initCssVars();
}
