import * as theme from "../service/theme";

export default function Icon(props) {
    let iconName = props.name;
    if (props.specialIconForDarkTheme && theme.isDark) {
        iconName = props.name + `-dark`;
    }
    return (
        <img src={`./images/${iconName}.svg`} alt="" style={props.style}/>
    )
}
