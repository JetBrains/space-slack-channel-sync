export default function Icon(props) {
    let iconName = props.name;
    if (props.specialIconForDarkTheme) {
        iconName = props.name + `-dark`;
    }
    return (
        <img src={`./images/${iconName}.svg`} alt="" style={props.style}/>
    )
}
