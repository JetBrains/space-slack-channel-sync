import * as theme from "../service/theme";

interface IconProps {
    name: string;
    specialIconForDarkTheme?: boolean;
    style?: object;
}

export default function Icon(props: IconProps) {
    let iconName = props.name;
    if (props.specialIconForDarkTheme && theme.isDark) {
        iconName = props.name + `-dark`;
    }
    return (
        <img src={`./images/${iconName}.svg`} alt="" style={props.style}/>
    )
}
