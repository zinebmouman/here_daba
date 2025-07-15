import React from "react";
import SimpleBar from "simplebar-react";
import "simplebar-react/dist/simplebar.min.css";

interface CustomScrollbarProps {
  children: React.ReactNode;
  style?: React.CSSProperties;
}

const CustomScrollbar: React.FC<CustomScrollbarProps> = ({
  children,
  style,
}) => {
  return (
    <SimpleBar
      style={{
        height: "100vh",
        width: "100%",
        ...style,
      }}
      classNames={{
        // Apply custom classes for styling
        scrollbar: "custom-scrollbar",
        track: "custom-scrollbar-track",
        //thumb: "custom-scrollbar-thumb",
      }}
    >
      {children}
    </SimpleBar>
  );
};

// Add this CSS to your global stylesheet or create a separate CSS module
/*
.custom-scrollbar .simplebar-scrollbar::before {
  background-color: #c1c1c1;
  width: 8px;
  border-radius: 4px;
  opacity: 1;
}

.custom-scrollbar .simplebar-track.simplebar-vertical {
  background-color: #f1f1f1;
  width: 8px;
  border-radius: 4px;
  right: 0;
}

.custom-scrollbar .simplebar-track.simplebar-vertical .simplebar-scrollbar:before {
  top: 2px;
  bottom: 2px;
}
*/

export default CustomScrollbar;


