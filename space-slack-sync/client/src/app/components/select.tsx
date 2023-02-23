import './select.css';
import * as theme from "../service/theme";
import AsyncSelect from "react-select/async";
import { components, OptionProps, StylesConfig, Theme, ValueContainerProps } from "react-select";
import ChannelIcon from "./channelIcon";
import * as utils from "../service/utils";
import React, { ComponentType } from 'react';

export interface ChannelSelectOption {
    value: string;
    label: string;
    optionType: string;
    icon: string | null;
}

export type OptionsCallback = (options: ChannelSelectOption[]) => void;

interface ChannelSelectFieldProps {
    defaultOptions: ChannelSelectOption[];
    loadOptions: (query: string, callback: OptionsCallback) => void;
    onChange: (option: ChannelSelectOption | null) => void;
    value: ChannelSelectOption | null;
}

export default function ChannelSelectField(props: ChannelSelectFieldProps) {
    return <>
        <div className="channelSelectField">
            <AsyncSelect
                defaultOptions={props.defaultOptions}
                styles={customStyles}
                placeholder="Type to filter channels"
                loadOptions={utils.debounce((query: string, callback: OptionsCallback) => props.loadOptions(query, callback))}
                theme={(selectTheme: Theme) => ({
                    ...selectTheme,
                    borderRadius: 0,
                    colors: {
                        ...selectTheme.colors,
                        primary: theme.cssVars.get('--text-color-20') || selectTheme.colors.primary,
                        primary50: theme.cssVars.get('--list-item-hover-background-color')  || selectTheme.colors.primary50,
                    },
                })}
                components={{ Option, ValueContainer }}
                onChange={(newValue: unknown) => props.onChange(newValue as ChannelSelectOption)}
                value={props.value}
            />
        </div>
    </>
}

const Option = (props: OptionProps) => {
    const classNames = props.isSelected || props.isFocused ? "selectOption selectOptionFocused" : "selectOption selectOptionNotFocused";
    const option = props.data as ChannelSelectOption;
    return <>
        <div className={classNames}>
            <ChannelIcon
                optionType={option.optionType}
                iconUrl={option.icon}
            />

            <components.Option {...props} />
        </div>
    </>
};

const ValueContainer: ComponentType<ValueContainerProps<unknown, false>> = ({ children, ...props }) => {
    let value = props.selectProps.value as ChannelSelectOption;

    return <div className="selectOption">
        {
            value != null &&
            <ChannelIcon
                optionType={value.optionType}
                iconUrl={value.icon}
            />
        }
        <components.ValueContainer {...props}>{children}</components.ValueContainer>
    </div>
};

const customStyles: StylesConfig = {
    option: (provided, state) => ({
        ...provided,
        color: theme.cssVars.get('--text-color'),
        backgroundColor: state.isFocused ? theme.cssVars.get('--list-item-hover-background-color') : theme.cssVars.get('--background-color'),
    }),
    input: (provided, state) => ({
        ...provided,
        color: theme.cssVars.get('--input-text-color'),
        fontWeight: theme.cssVars.get('--input-font-weight'),
    }),
    dropdownIndicator: (provided, state) => ({
        ...provided,
        color: theme.cssVars.get('--text-color-20'),
    }),
    indicatorSeparator: (provided, state) => ({
        ...provided,
        backgroundColor: theme.cssVars.get('--text-color-20'),
    }),
    placeholder: (provided, state) => ({
        ...provided,
        color: theme.cssVars.get('--input-placeholder-color'),
        fontWeight: theme.cssVars.get('--input-font-weight'),
    }),
    control: (provided, state) => ({
        ...provided,
        borderStyle: 'solid',
        borderWidth: '1px',
        borderColor: theme.cssVars.get('--text-color-20'),
        backgroundColor: theme.cssVars.get('--input-background-color'),
        height: `42px`,
    }),
    menu: (provided, state) => ({
        ...provided,
        backgroundColor: theme.cssVars.get('--background-color'),
    }),
    singleValue: (provided, state) => ({
        ...provided,
        color: theme.cssVars.get('--input-text-color'),
        fontWeight: theme.cssVars.get('--input-font-weight'),
    }),
}
