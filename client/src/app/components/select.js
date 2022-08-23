import './select.css';
import * as theme from "../service/theme";
import AsyncSelect from "react-select/async";
import {components} from "react-select";
import ChannelIcon from "./channelIcon";
import * as utils from "../service/utils";

export default function ChannelSelectField(props) {
    return (
        <div className="channelSelectField">
            <AsyncSelect
                defaultOptions={props.defaultOptions}
                styles={customStyles}
                placeholder="Type to filter channels"
                loadOptions={utils.debounce((query, callback) => props.loadOptions(query, callback))}
                theme={(selectTheme) => ({
                    ...selectTheme,
                    borderRadius: 0,
                    colors: {
                        ...selectTheme.colors,
                        primary: theme.cssVars['--text-color-20'],
                        primary50: theme.cssVars['--list-item-hover-background-color'],
                    },
                })}
                components={{Option, ValueContainer}}
                onChange={props.onChange}
                value={props.value}
            />
        </div>
    );
}

const Option = (props) => {
    let classNames = props.isSelected || props.isFocused ? "selectOption selectOptionFocused" : "selectOption selectOptionNotFocused";
    return (
        <div className={classNames}>
            <ChannelIcon
                optionType={props.data.optionType}
                iconUrl={props.data.icon}
            />

            <components.Option {...props} />
        </div>
    );
};


const ValueContainer = ({children, ...props}) => {
    let value = props.selectProps.value;

    return (
        <div className="selectOption">
            {
                value != null &&
                <ChannelIcon
                    optionType={value.optionType}
                    iconUrl={value.icon}
                />
            }
            <components.ValueContainer {...props}>{children}</components.ValueContainer>
        </div>
    );
};

const customStyles = {
    option: (provided, state) => ({
        ...provided,
        color: theme.cssVars['--text-color'],
        backgroundColor: state.isFocused ? theme.cssVars['--list-item-hover-background-color'] : theme.cssVars['--background-color'],
    }),
    input: (provided, state) => ({
        ...provided,
        color: theme.cssVars['--input-text-color'],
        fontWeight: theme.cssVars['--input-font-weight'],
    }),
    dropdownIndicator: (provided, state) => ({
        ...provided,
        color: theme.cssVars['--text-color-20'],
    }),
    indicatorSeparator: (provided, state) => ({
        ...provided,
        backgroundColor: theme.cssVars['--text-color-20'],
    }),
    placeholder: (provided, state) => ({
        ...provided,
        color: theme.cssVars['--input-placeholder-color'],
        fontWeight: theme.cssVars['--input-font-weight'],
    }),
    control: (provided, state) => ({
        ...provided,
        borderStyle: 'solid',
        borderWidth: '1px',
        borderColor: theme.cssVars['--text-color-20'],
        backgroundColor: theme.cssVars['--input-background-color'],
        height: `42px`,
    }),
    menu: (provided, state) => ({
        ...provided,
        backgroundColor: theme.cssVars['--background-color'],
    }),
    singleValue: (provided, state) => ({
        ...provided,
        color: theme.cssVars['--input-text-color'],
        fontWeight: theme.cssVars['--input-font-weight'],
    }),
}
