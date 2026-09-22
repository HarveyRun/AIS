export function CountryFlag({
  countryId,
  flag,
  className = "h-3 w-4",
}: {
  countryId: string;
  /** 国旗 emoji，作为图片加载失败时的回退显示 */
  flag: string;
  className?: string;
}) {
  return (
    <img
      src={`/flags/${countryId}.svg`}
      alt={flag}
      loading="lazy"
      className={`inline-block shrink-0 rounded-[2px] object-cover align-[-1px] ${className}`}
    />
  );
}
