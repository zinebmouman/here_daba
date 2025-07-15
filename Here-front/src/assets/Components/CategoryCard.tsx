import { LucideIcon } from "lucide-react";
import React from "react";

interface CategoriecardProps {
    Icon:LucideIcon;
    Label:string;
    onClick?: () =>void;
}

const CategoryCard: React.FC<CategoriecardProps> = ({Icon,Label,onClick}) => {
    return (
        <>
                <div onClick={onClick} className="catcard flex flex-col items-center justify-center p-4 transition-colors cursor-pointer all">
                    <div className="mb-2 iconcard">
                        <Icon className="h-8 w-8"/>
                    </div>
                    <span className="text-sm font-semibold">{Label}</span>
                </div>
        </>
    );
};
export default CategoryCard