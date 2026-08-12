import { useEffect, useState } from 'react';
import { BadgeCheck, UserRound } from 'lucide-react';

export default function UserAvatar({ src, uid, name, className = 'avatar', verified = false }) {
  const [imageFailed, setImageFailed] = useState(false);
  const normalizedSrc = src && !/^(?:https?:|data:|blob:|\/)/i.test(src) ? `/${src}` : src;
  const showImage = Boolean(normalizedSrc) && !imageFailed;

  useEffect(() => {
    setImageFailed(false);
  }, [normalizedSrc]);

  return (
    <div className={`${className}${showImage ? '' : ' default-avatar'}`}>
      {showImage ? (
        <img
          src={normalizedSrc}
          alt={`${name || (uid ? `UID ${uid}` : '用户')}的头像`}
          onError={() => setImageFailed(true)}
        />
      ) : (
        <UserRound aria-label="默认头像" />
      )}
      {verified && (
        <i>
          <BadgeCheck aria-hidden="true" />
        </i>
      )}
    </div>
  );
}
