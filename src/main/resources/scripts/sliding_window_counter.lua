local currentCountStr = redis.call('GET', KEYS[1])
local currentCount = 0
if currentCountStr then
    currentCount = tonumber(currentCountStr)
end

local prevCountStr = redis.call('GET', KEYS[2])
local prevCount = 0
if prevCountStr then
    prevCount = tonumber(prevCountStr)
end

local elapsedInCurrentWindow = tonumber(ARGV[3]) % tonumber(ARGV[2])
local weight = (tonumber(ARGV[2]) - elapsedInCurrentWindow) / tonumber(ARGV[2])

local total = currentCount + (prevCount * weight)

if total < tonumber(ARGV[4]) then
    local count = redis.call('INCR', KEYS[1])
    if count == 1 then
        redis.call('PEXPIRE', KEYS[1], ARGV[1])
    end
    return 1
end

return 0