const Skeleton = ({ className = '' }) => {
  return (
    <div className={`bg-lavender/70 animate-pulse rounded-xl ${className}`} />
  );
};

export const ProductCardSkeleton = () => {
  return (
    <div className="bg-white rounded-2xl overflow-hidden">
      <Skeleton className="aspect-square m-1.5 rounded-2xl" />
      <div className="px-3 pb-3 pt-2 space-y-2">
        <Skeleton className="h-3.5 w-4/5 rounded" />
        <Skeleton className="h-4 w-1/3 rounded" />
        <Skeleton className="h-3 w-2/3 rounded" />
      </div>
    </div>
  );
};

export const ProductGridSkeleton = ({ count = 6 }) => {
  return (
    <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
      {Array.from({ length: count }).map((_, i) => (
        <ProductCardSkeleton key={i} />
      ))}
    </div>
  );
};

export const ListRowSkeleton = () => {
  return (
    <div className="flex items-center gap-4 p-4">
      <Skeleton className="w-11 h-11 rounded-[1.20rem]" />
      <div className="flex-1 space-y-2">
        <Skeleton className="h-3.5 w-2/5 rounded" />
        <Skeleton className="h-3 w-3/5 rounded" />
      </div>
      <Skeleton className="h-3 w-8 rounded" />
    </div>
  );
};

export default Skeleton;
