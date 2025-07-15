import React, { ChangeEvent, InputHTMLAttributes, forwardRef } from 'react';

interface InputProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'className'> {
  label?: string;
  error?: string;
  helperText?: string;
  startIcon?: React.ReactNode;
  endIcon?: React.ReactNode;
  fullWidth?: boolean;
  variant?: 'outlined' | 'filled' | 'standard';
  inputSize?: 'small' | 'medium' | 'large';
  containerClassName?: string;
  inputClassName?: string;
  labelClassName?: string;
}

const Input = forwardRef<HTMLInputElement, InputProps>(
  (
    {
      label,
      error,
      helperText,
      startIcon,
      endIcon,
      fullWidth = false,
      variant = 'outlined',
      size = 'medium',
      containerClassName = '',
      inputClassName = '',
      labelClassName = '',
      disabled,
      required,
      ...props
    },
    ref
  ) => {
    // Determine sizes based on the size prop
    const getSizeClasses = () => {
      switch (size) {
        case 'small':
          return 'py-1 px-2 text-sm';
        case 'large':
          return 'py-3 px-4 text-lg';
        default:
          return 'py-2 px-3 text-base';
      }
    };

    // Determine variant styles
    const getVariantClasses = () => {
      switch (variant) {
        case 'filled':
          return error
            ? 'bg-red-50 border-red-500 focus:ring-red-500'
            : 'bg-gray-100 border-gray-300 focus:border-teal-500 focus:ring-teal-500';
        case 'standard':
          return error
            ? 'border-b border-red-500 rounded-none px-0 focus:ring-0 focus:border-red-500'
            : 'border-b border-gray-300 rounded-none px-0 focus:ring-0 focus:border-teal-500';
        default:
          return error
            ? 'bg-white border-red-500 focus:ring-red-500'
            : 'bg-white border-gray-300 focus:border-teal-500 focus:ring-teal-500';
      }
    };

    return (
      <div className={`${fullWidth ? 'w-full' : 'max-w-md'} ${containerClassName}`}>
        {label && (
          <label
            htmlFor={props.id || props.name}
            className={`block text-sm font-medium text-gray-700 mb-1 ${labelClassName} ${
              required ? 'after:content-["*"] after:ml-0.5 after:text-red-500' : ''
            }`}
          >
            {label}
          </label>
        )}

        <div className="relative">
          {startIcon && (
            <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
              {startIcon}
            </div>
          )}

          <input
            ref={ref}
            className={`appearance-none ${fullWidth ? 'w-full' : ''} ${getSizeClasses()} ${getVariantClasses()} 
              ${
                startIcon ? 'pl-10' : ''
              } ${
                endIcon ? 'pr-10' : ''
              } border rounded-md focus:outline-none focus:ring-2 
              ${disabled ? 'bg-gray-100 text-gray-500 cursor-not-allowed' : ''}
              ${inputClassName}`}
            disabled={disabled}
            required={required}
            aria-invalid={error ? 'true' : 'false'}
            aria-describedby={helperText ? `${props.id || props.name}-helper-text` : undefined}
            {...props}
          />

          {endIcon && (
            <div className="absolute inset-y-0 right-0 pr-3 flex items-center pointer-events-none">
              {endIcon}
            </div>
          )}
        </div>

        {(error || helperText) && (
          <p
            id={`${props.id || props.name}-helper-text`}
            className={`mt-1 text-sm ${error ? 'text-red-600' : 'text-gray-500'}`}
          >
            {error || helperText}
          </p>
        )}
      </div>
    );
  }
);

Input.displayName = 'Input';

export default Input;