import './logoWithNameAndHost.css';

export default function LogoWithNameAndHost(props) {
    return (
        <div className="logoWithNameAndHost">
            {props.renderLogo()}
            <div className="nameAndHost">
                <span className="logoName">{props.name}</span>
                <span className="logoHost">{props.host}</span>
            </div>
        </div>
    );
}
