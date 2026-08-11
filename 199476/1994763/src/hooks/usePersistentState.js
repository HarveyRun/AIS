import { useEffect, useState } from 'react';

export default function usePersistentState(key, initialValue) {
  const [value, setValue] = useState(() => {
    try {
      const savedValue = localStorage.getItem(key);
      return savedValue === null
        ? typeof initialValue === 'function'
          ? initialValue()
          : initialValue
        : JSON.parse(savedValue);
    } catch {
      return typeof initialValue === 'function' ? initialValue() : initialValue;
    }
  });

  useEffect(() => {
    try {
      localStorage.setItem(key, JSON.stringify(value));
    } catch {
      // 存储空间不足时保留当前页面内的数据，不中断用户操作。
    }
  }, [key, value]);

  return [value, setValue];
}
